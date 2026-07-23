import { mkdir, writeFile } from 'node:fs/promises'

const outputDir = new URL('../output/playwright/', import.meta.url)
await mkdir(outputDir, { recursive: true })

const loginResponse = await fetch('http://127.0.0.1:8080/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json; charset=utf-8' },
  body: JSON.stringify({ username: 'demo_student', password: '123456' })
})
const login = await loginResponse.json()
if (!login.success || !login.data?.accessToken) throw new Error(`Login failed: ${JSON.stringify(login)}`)

const targets = await fetch('http://127.0.0.1:9225/json/list').then((response) => response.json())
const target = targets.find((item) => item.type === 'page')
if (!target) throw new Error('No Chrome page target found')

const ws = new WebSocket(target.webSocketDebuggerUrl)
await new Promise((resolve, reject) => {
  ws.onopen = resolve
  ws.onerror = reject
})

let nextId = 1
const pending = new Map()
const events = new Map()
const consoleErrors = []

ws.onmessage = ({ data }) => {
  const message = JSON.parse(data)
  if (message.id) {
    const waiter = pending.get(message.id)
    if (!waiter) return
    pending.delete(message.id)
    if (message.error) waiter.reject(new Error(message.error.message))
    else waiter.resolve(message.result)
    return
  }
  if (message.method === 'Runtime.exceptionThrown') {
    consoleErrors.push(message.params.exceptionDetails?.exception?.description || message.params.exceptionDetails?.text)
  }
  if (message.method === 'Log.entryAdded' && message.params.entry.level === 'error') {
    consoleErrors.push(message.params.entry.text)
  }
  const listeners = events.get(message.method)
  if (listeners?.length) listeners.splice(0).forEach((resolve) => resolve(message.params))
}

function send(method, params = {}) {
  const id = nextId++
  return new Promise((resolve, reject) => {
    pending.set(id, { resolve, reject })
    ws.send(JSON.stringify({ id, method, params }))
  })
}

function once(method, timeout = 10000) {
  return new Promise((resolve, reject) => {
    const listeners = events.get(method) || []
    listeners.push(resolve)
    events.set(method, listeners)
    setTimeout(() => reject(new Error(`Timeout waiting for ${method}`)), timeout)
  })
}

async function evaluate(expression) {
  const result = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.exception?.description || result.exceptionDetails.text)
  return result.result.value
}

async function navigate(path) {
  const loaded = once('Page.loadEventFired')
  await send('Page.navigate', { url: `http://127.0.0.1:5173${path}` })
  await loaded
  await new Promise((resolve) => setTimeout(resolve, 1400))
}

async function screenshot(name) {
  const result = await send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false })
  await writeFile(new URL(name, outputDir), Buffer.from(result.data, 'base64'))
}

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

async function formValues() {
  return evaluate(`(() => Object.fromEntries([...document.querySelectorAll('.el-form-item')].map((item) => {
    const label = item.querySelector('.el-form-item__label')?.textContent?.trim()
    const field = item.querySelector('input:not([type="hidden"]), textarea')
    return label && field ? [label, field.value] : null
  }).filter(Boolean)))()`)
}

await send('Page.enable')
await send('Runtime.enable')
await send('Log.enable')
await send('Emulation.setDeviceMetricsOverride', {
  width: 1440,
  height: 1000,
  deviceScaleFactor: 1,
  mobile: false
})

await navigate('/login')
await evaluate(`localStorage.setItem('eduagent_token', ${JSON.stringify(login.data.accessToken)}); localStorage.setItem('eduagent_user', ${JSON.stringify(JSON.stringify(login.data.user || {}))}); localStorage.setItem('eduagent_theme', 'light')`)

const report = {}

await navigate('/chat')
report.chat = await evaluate(`(() => {
  const list = document.querySelector('.conversation-list')?.getBoundingClientRect()
  const windowRect = document.querySelector('.chat-window')?.getBoundingClientRect()
  const text = document.body.innerText
  return {
    listWidth: list?.width,
    windowWidth: windowRect?.width,
    duplicateHeaderCount: document.querySelectorAll('.chat-page > .page-header, .chat-page > .el-card').length,
    hasProfileBanner: text.includes('本次对话正在参考学习画像'),
    hasQuickPrompts: !!document.querySelector('.quick-prompts, .examples'),
    hasMessages: !!document.querySelector('.messages'),
    hasSender: !!document.querySelector('.sender'),
    horizontalOverflow: document.body.scrollWidth > innerWidth
  }
})()`)
assert(report.chat.listWidth <= 230, `Conversation list is still too wide: ${report.chat.listWidth}`)
assert(report.chat.windowWidth >= 700, `Chat window is too narrow: ${report.chat.windowWidth}`)
assert(report.chat.duplicateHeaderCount === 0, 'Duplicate chat page header is still visible')
assert(!report.chat.hasProfileBanner, 'Profile banner was not removed from chat')
assert(!report.chat.hasQuickPrompts, 'Quick prompts were not removed from chat')
assert(report.chat.hasMessages && report.chat.hasSender, 'Chat messages or sender is missing')
assert(!report.chat.horizontalOverflow, 'Chat page has horizontal overflow')
await screenshot('chat-layout.png')

await navigate('/knowledge')
report.knowledge = await evaluate(`(() => {
  const input = document.querySelector('input[type="file"]')
  return {
    fileInputPresent: !!input,
    multiple: !!input?.multiple,
    duplicateHeaderCount: document.querySelectorAll('.page > .page-header').length,
    horizontalOverflow: document.body.scrollWidth > innerWidth
  }
})()`)
assert(report.knowledge.fileInputPresent && report.knowledge.multiple, 'Knowledge upload is not configured for multiple files')
assert(report.knowledge.duplicateHeaderCount === 0, 'Duplicate knowledge page header is still visible')
assert(!report.knowledge.horizontalOverflow, 'Knowledge page has horizontal overflow')
await screenshot('knowledge-multiselect.png')

await navigate('/resource-generation')
const resourceValues = await formValues()
report.resourceGeneration = await evaluate(`(() => ({
  typeCount: document.querySelectorAll('.type-card').length,
  duplicateHeaderCount: document.querySelectorAll('.page > .page-header').length,
  horizontalOverflow: document.body.scrollWidth > innerWidth
}))()`)
report.resourceGeneration.values = resourceValues
assert(report.resourceGeneration.typeCount === 8, `Expected 8 resource types, found ${report.resourceGeneration.typeCount}`)
for (const label of ['资源标题', '学科', '重点内容']) assert(!resourceValues[label], `${label} should start empty`)
assert(report.resourceGeneration.duplicateHeaderCount === 0, 'Duplicate resource page header is still visible')
await screenshot('resource-generator-empty.png')

const emptyFieldRoutes = [
  { path: '/agents', name: 'agent', labels: ['任务标题', '学科', '知识点'], screenshot: 'agent-workspace-empty.png' },
  { path: '/paths', name: 'path', labels: ['学科', '目标', '知识点'], screenshot: 'learning-path-empty.png' },
  { path: '/quiz', name: 'quiz', labels: ['学科', '标题', '知识点'], screenshot: 'quiz-empty.png' }
]

for (const route of emptyFieldRoutes) {
  await navigate(route.path)
  const values = await formValues()
  const metrics = await evaluate(`(() => ({
    duplicateHeaderCount: document.querySelectorAll('.page > .page-header').length,
    horizontalOverflow: document.body.scrollWidth > innerWidth
  }))()`)
  for (const label of route.labels) assert(!values[label], `${route.name} field ${label} should start empty`)
  assert(metrics.duplicateHeaderCount === 0, `Duplicate ${route.name} page header is still visible`)
  assert(!metrics.horizontalOverflow, `${route.name} page has horizontal overflow`)
  report[route.name] = { values, ...metrics }
  await screenshot(route.screenshot)
}

report.consoleErrors = consoleErrors
assert(consoleErrors.length === 0, `Browser console errors: ${consoleErrors.join(' | ')}`)
await writeFile(new URL('experience-report.json', outputDir), JSON.stringify(report, null, 2))
console.log(JSON.stringify(report, null, 2))
ws.close()
