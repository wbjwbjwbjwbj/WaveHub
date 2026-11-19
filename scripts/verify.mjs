import { spawn } from 'node:child_process'
import { existsSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const { loadEnv } = await import(pathToFileURL(resolve(root, 'frontend/node_modules/vite/dist/node/index.js')))
const local = loadEnv('development', root, '')
for (const name of ['DB_URL', 'DB_USERNAME', 'DB_PASSWORD']) {
  if (!local[name] || /^<.*>$/.test(local[name])) throw new Error(`请在根目录 .env 中配置 ${name}`)
}
if (!process.env.JAVA_HOME || !existsSync(resolve(process.env.JAVA_HOME, 'bin/java.exe'))) {
  throw new Error('请先将 JAVA_HOME 设置为 JDK 21 的目录')
}
const env = {
  ...process.env, ...local,
  JAVA_HOME: process.env.JAVA_HOME,
  MAVEN_USER_HOME: resolve(root, '.cache/maven-user'),
  WAVEHUB_MVN_SCRIPT: resolve(root, 'backend/mvnw.cmd'),
  WAVEHUB_MVN_REPO: resolve(root, '.cache/maven'),
  WAVEHUB_DB_TEST: 'true',
}
// 测试类分别指定 standalone / kingbase，不继承运行应用时的默认 profile。
delete env.SPRING_PROFILES_ACTIVE

function run(command, cwd) {
  return new Promise((resolveRun, reject) => {
    const child = spawn('powershell.exe', ['-NoProfile', '-Command', command], { cwd, env, windowsHide: true, stdio: 'inherit' })
    child.on('error', reject)
    child.on('exit', code => code === 0 ? resolveRun() : reject(new Error(`检查失败，退出码 ${code}`)))
  })
}

try {
  console.log('Checking frontend types, lint, format and build...')
  await run('& pnpm lint; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }; & pnpm format:check; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }; & pnpm build; exit $LASTEXITCODE', resolve(root, 'frontend'))
  console.log('Checking backend and real KingbaseES CRUD with rollback...')
  await run("& $env:WAVEHUB_MVN_SCRIPT '-B' '-ntp' ('-Dmaven.repo.local=' + $env:WAVEHUB_MVN_REPO) 'verify'; exit $LASTEXITCODE", resolve(root, 'backend'))
  console.log('Frontend, backend and KingbaseES checks passed.')
} catch (error) {
  console.error(error.message)
  process.exitCode = 1
}
