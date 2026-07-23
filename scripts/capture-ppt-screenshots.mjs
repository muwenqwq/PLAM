import { createRequire } from 'node:module'
import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)
const { chromium } = require('playwright')

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const outDir = path.join(root, 'ppt_assets', 'screenshots')
await fs.mkdir(outDir, { recursive: true })

const loginResp = await fetch('http://127.0.0.1:8080/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json; charset=utf-8' },
  body: JSON.stringify({ username: 'demo_student', password: '123456' })
})
if (!loginResp.ok) throw new Error(`Login request failed: ${loginResp.status}`)
const loginBody = await loginResp.json()
if (!loginBody.success) throw new Error(loginBody.message || 'Login failed')
const { accessToken, user } = loginBody.data

const browser = await chromium.launch({
  executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe',
  headless: true
})
const page = await browser.newPage({ viewport: { width: 1600, height: 1000 }, deviceScaleFactor: 1 })

async function openAuthed(route) {
  await page.goto('http://127.0.0.1:5173/login', { waitUntil: 'networkidle' })
  await page.evaluate(({ accessToken, user }) => {
    localStorage.setItem('eduagent_token', accessToken)
    localStorage.setItem('eduagent_user', JSON.stringify(user))
    localStorage.setItem('eduagent_theme', 'light')
  }, { accessToken, user })
  await page.goto(`http://127.0.0.1:5173${route}`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(900)
}

async function shot(name, route, fullPage = false) {
  await openAuthed(route)
  await page.screenshot({ path: path.join(outDir, `${name}.png`), fullPage })
  const text = await page.locator('body').innerText().catch(() => '')
  return { name, route, title: await page.title(), text: text.slice(0, 500) }
}

const pages = []
pages.push(await shot('01_home', '/', false))
pages.push(await shot('02_dashboard', '/dashboard', false))
pages.push(await shot('03_spaces', '/spaces', false))
pages.push(await shot('04_knowledge', '/knowledge', false))
pages.push(await shot('05_resource_generation', '/resource-generation', false))
pages.push(await shot('06_resources', '/resources', false))
pages.push(await shot('07_learning_path', '/paths', false))
pages.push(await shot('08_chat', '/chat', false))
pages.push(await shot('09_quiz', '/quiz', false))
pages.push(await shot('10_report', '/reports', false))
pages.push(await shot('11_agents', '/agents', false))

await fs.writeFile(path.join(outDir, 'capture-report.json'), JSON.stringify({ capturedAt: new Date().toISOString(), pages }, null, 2), 'utf8')
await browser.close()
console.log(JSON.stringify({ outDir, count: pages.length }, null, 2))
