import { mkdir, writeFile } from 'node:fs/promises'

const outputDir = new URL('../output/visual-review/', import.meta.url)
await mkdir(outputDir, { recursive: true })
const targets = await fetch('http://127.0.0.1:9225/json/list').then((r) => r.json())
const target = targets.find((item) => item.type === 'page')
if (!target) throw new Error('No Chrome page target found')
const ws = new WebSocket(target.webSocketDebuggerUrl)
await new Promise((resolve, reject) => { ws.onopen = resolve; ws.onerror = reject })
let nextId = 1
const pending = new Map()
const events = new Map()
const consoleErrors = []
let currentPath = ''
ws.onmessage = async ({ data }) => {
  const message = JSON.parse(data)
  if (message.id) {
    const waiter = pending.get(message.id)
    if (waiter) {
      pending.delete(message.id)
      if (message.error) waiter.reject(new Error(message.error.message))
      else waiter.resolve(message.result)
    }
    return
  }
  if (message.method === 'Runtime.exceptionThrown') {
    const detail = message.params.exceptionDetails || {}
    consoleErrors.push({ path: currentPath, text: detail.text || 'Runtime exception', description: detail.exception?.description, url: detail.url, line: detail.lineNumber, stack: detail.stackTrace?.callFrames?.slice(0, 4) })
  }
  if (message.method === 'Log.entryAdded' && message.params.entry.level === 'error') consoleErrors.push({ path: currentPath, text: message.params.entry.text, url: message.params.entry.url })
  if (message.method === 'Fetch.requestPaused') {
    const requestId = message.params.requestId
    const url = message.params.request.url
    let payload = { records: [], total: 0, pageNum: 1, pageSize: 50, pages: 0 }
    if (url.includes('/reports/overview')) payload = { spaceCount: 2, resourceCount: 6, quizCount: 3, pathCount: 1, averageScore: 86, averageMastery: 78 }
    else if (url.includes('/reports/space/')) payload = []
    else if (url.includes('/mastery/me')) payload = []
    else if (url.includes('/profiles/me')) payload = { learningGoal: '', weakPoints: [], availableTimeSlots: [] }
    else if (url.includes('/preferences/me')) payload = { preferredResourceTypes: [], contentLengthPreference: 'medium', difficultyPreference: 'medium', languagePreference: 'zh-CN' }
    else if (url.includes('/learning-spaces/default')) payload = { id: 1, spaceName: '软件工程期末复习', subject: '软件工程' }
    else if (url.includes('/learning-spaces')) payload = { records: [{ id: 1, spaceName: '软件工程期末复习', subject: '软件工程' }, { id: 2, spaceName: '数据库复习', subject: '数据库' }], total: 2, pageNum: 1, pageSize: 50, pages: 1 }
    else if (url.includes('/knowledge/files')) payload = { records: [{ id: 11, spaceId: 1, originalName: '软件工程复习指南.docx', fileType: 'docx', parserStatus: 'completed', chunkCount: 24, createdAt: '2026-06-21T14:20:00' }], total: 1, pageNum: 1, pageSize: 50, pages: 1 }
    else if (url.includes('/resources')) payload = { records: [{ id: 21, spaceId: 1, title: '软件需求复习笔记', resourceType: 'lecture_note', outputSummary: '需求分析、用例与规格说明复习要点', createdAt: '2026-06-21T14:30:00' }], total: 1, pageNum: 1, pageSize: 50, pages: 1 }
    const body = Buffer.from(JSON.stringify({ success: true, code: '0', message: 'ok', data: payload, timestamp: new Date().toISOString() })).toString('base64')
    send('Fetch.fulfillRequest', { requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: 'application/json; charset=utf-8' }], body }).catch(() => {})
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
    const list = events.get(method) || []
    list.push(resolve)
    events.set(method, list)
    setTimeout(() => reject(new Error(`Timeout waiting for ${method}`)), timeout)
  })
}
async function navigate(url) {
  currentPath = new URL(url).pathname
  const loaded = once('Page.loadEventFired')
  await send('Page.navigate', { url })
  await loaded
  await new Promise((resolve) => setTimeout(resolve, 800))
}
async function evaluate(expression) {
  const result = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
  return result.result.value
}
async function screenshot(name) {
  const result = await send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false })
  await writeFile(new URL(name, outputDir), Buffer.from(result.data, 'base64'))
}
async function metrics() {
  return evaluate(`(() => ({
    path: location.pathname,
    theme: document.documentElement.dataset.theme,
    title: document.title,
    bodyWidth: document.body.scrollWidth,
    viewportWidth: innerWidth,
    bodyHeight: document.body.scrollHeight,
    viewportHeight: innerHeight,
    h1: document.querySelector('h1')?.textContent?.trim(),
    navItems: document.querySelectorAll('.el-menu-item').length,
    cards: document.querySelectorAll('.el-card, .study-action, .stat-card').length,
    themeButton: !!document.querySelector('.theme-toggle'),
    visibleText: document.body.innerText.slice(0, 240)
  }))()`)
}

await send('Page.enable')
await send('Runtime.enable')
await send('Log.enable')
await send('Fetch.enable', { patterns: [{ urlPattern: 'http://127.0.0.1:5173/api/*', requestStage: 'Request' }] })
await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false })

const report = {}
await navigate('http://127.0.0.1:5173/login')
await evaluate("localStorage.clear()")
await navigate('http://127.0.0.1:5173/')
report.home = await metrics()
await screenshot('home-light.png')
await navigate('http://127.0.0.1:5173/login')
report.login = await metrics()
await screenshot('login-light.png')
await evaluate(`localStorage.setItem('eduagent_token','visual-test'); localStorage.setItem('eduagent_user', JSON.stringify({username:'demo_student',nickname:'Demo Student'})); localStorage.setItem('eduagent_theme','light')`)
await navigate('http://127.0.0.1:5173/dashboard')
report.dashboardLight = await metrics()
await screenshot('dashboard-light.png')
await evaluate(`document.querySelector('.theme-toggle')?.click()`)
await new Promise((resolve) => setTimeout(resolve, 500))
report.dashboardDark = await metrics()
report.persistedTheme = await evaluate(`localStorage.getItem('eduagent_theme')`)
await screenshot('dashboard-dark.png')
await navigate('http://127.0.0.1:5173/knowledge')
report.knowledgeDark = await metrics()
await screenshot('knowledge-dark.png')
for (const route of ['resource-generation', 'resources', 'quiz', 'reports', 'profile']) {
  await navigate('http://127.0.0.1:5173/' + route)
  report[route] = await metrics()
  await screenshot(route + '-dark.png')
}
await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true, screenWidth: 390, screenHeight: 844 })
await navigate('http://127.0.0.1:5173/dashboard')
report.dashboardMobile = await metrics()
await evaluate("document.querySelector('.mobile-menu-button')?.click()")
await new Promise((resolve) => setTimeout(resolve, 250))
report.mobileMenu = await evaluate("(() => { const sidebar = document.querySelector('.sidebar'); const rect = sidebar?.getBoundingClientRect(); return { open: sidebar?.classList.contains('mobile-open'), left: rect?.left, right: rect?.right, width: rect?.width } })()")
await screenshot('dashboard-mobile-dark.png')
report.consoleErrors = consoleErrors
await writeFile(new URL('report.json', outputDir), JSON.stringify(report, null, 2))
console.log(JSON.stringify(report, null, 2))
ws.close()
