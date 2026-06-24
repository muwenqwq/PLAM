import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const root = new URL('../', import.meta.url)

async function read(path) {
  return readFile(new URL(path, root), 'utf8')
}

const [store, storage, styles, layout, login, register, generator, resources, report, mermaid] = await Promise.all([
  read('src/stores/app.ts'),
  read('src/utils/storage.ts'),
  read('src/assets/main.css'),
  read('src/layout/BasicLayout.vue'),
  read('src/views/Login.vue'),
  read('src/views/Register.vue'),
  read('src/views/ResourceGenerator.vue'),
  read('src/views/MyResources.vue'),
  read('src/views/Report.vue'),
  read('src/components/MermaidViewer.vue')
])

assert.match(store, /theme:\s*readStoredTheme\(\)/, 'app store should initialize theme from storage')
assert.match(store, /document\.documentElement\.dataset\.theme/, 'theme should be applied to the root element')
assert.match(store, /setStorageItem\(['"]eduagent_theme['"]/, 'theme should persist through safe storage')
assert.match(storage, /try\s*\{[\s\S]*localStorage\.getItem/, 'storage reads should tolerate restricted browser storage')
assert.match(storage, /JSON\.parse[\s\S]*catch/, 'corrupt stored user data should not crash startup')
assert.match(styles, /:root\[data-theme=['"]dark['"]\]/, 'global styles should define a dark theme')
assert.match(styles, /--surface-glass:/, 'global styles should expose a shared glass surface token')
assert.match(styles, /--el-color-success-light-9:/, 'dark theme should define semantic Element Plus shades')
assert.match(layout, /ThemeToggle/, 'student app shell should expose the theme switch')
assert.match(layout, /path: '\/models'/, 'student navigation should expose AI model settings')
assert.match(layout, /aria-controls="student-sidebar"/, 'mobile menu button should identify its controlled navigation')
assert.match(layout, /:aria-expanded="app\.mobileMenuOpen"/, 'mobile menu button should expose expanded state')
assert.match(layout, /:inert=/, 'off-screen mobile navigation should not remain keyboard accessible')
assert.match(login, /ThemeToggle/, 'login page should expose the theme switch')
assert.match(login, /if \(loading\.value\) return/, 'login should prevent duplicate submissions')
assert.match(login, /validate\(\)\.catch\(\(\) => false\)/, 'login validation should not leak rejected promises')
assert.match(register, /ThemeToggle/, 'register page should expose the theme switch')
assert.match(register, /validate\(\)\.catch\(\(\) => false\)/, 'register validation should not leak rejected promises')
assert.match(generator, /sourceFileIds:\s*selectedFileIds\.value/, 'normal generation should send selected file ids')
assert.match(resources, /route\.query\.edit/, 'resource list should open the requested edit dialog')
assert.match(report, /ResizeObserver/, 'report chart should resize with the layout')
assert.match(report, /document\.execCommand\(['"]copy['"]\)/, 'report export should provide a clipboard fallback')
assert.match(mermaid, /container\.value\.innerHTML = ''[\s\S]*if \(!props\.code\?\.trim\(\)\) return/, 'empty Mermaid input should clear stale output')

console.log('UI theme contract passed')