# 运行与验证

## 本地配置

真实配置保存在项目根目录 `.env`，已被 Git 忽略；`backend/.env.example` 为可提交的占位模板。
账号和密码值不需要包裹 `< >`，含特殊字符时使用 dotenv 支持的引号。

```dotenv
SPRING_PROFILES_ACTIVE=kingbase
DB_URL=jdbc:kingbase8://127.0.0.1:54321/WaveHubDB
DB_USERNAME=<实际数据库用户名>
DB_PASSWORD=<实际数据库密码>
WAVEHUB_ADMIN_USERNAME=admin
WAVEHUB_ADMIN_PASSWORD=<本地管理账号密码>
```

本次已将用户提供的连接值移至根目录 `.env`，并生成随机的本地管理密码。用户名和密码在该文件中查看、修改；不会在页面或日志中显示。

## 运行

VS Code 使用现有 `.vscode/launch.json` 启动 `WaveHubApplication`，它会加载根目录 `.env`。项目使用 Java 21。
前端在 `frontend` 目录执行 `pnpm dev`，默认访问 `http://127.0.0.1:5173`，登录后进入台站管理。
后端默认端口 8080；端口变更时调整前端 `API_PROXY_TARGET`。

命令行直接执行 Maven 或 java 不会自动读取 `.env`，应先在当前进程设置文件中的环境变量。

## 每次修改后的检查

在项目根目录 PowerShell 中执行：

```powershell
$env:JAVA_HOME = 'C:\Users\wang\.jdks\corretto-21.0.4'
node scripts/verify.mjs
```

脚本自动加载根目录 `.env`，依次执行：

1. 前端 lint、格式检查、TypeScript 类型检查和生产构建。
2. 后端认证、CSRF、公开接口及未认证拦截测试。
3. 连接真实金仓，确认库名为 `WaveHubDB`，运行台站和传感器 CRUD、编码冲突、跨站归属、输入校验、引用保护及可空字段清空测试。
4. 回滚测试写入，检查带测试前缀的台站和文件记录不存在，然后打包后端。

真实数据库测试仅写入本次创建的测试对象，不修改用户已有记录。测试所需 11 张业务表应先在当前 schema 中建立。
任一环节失败，脚本以非零退出码结束。缺少配置时会明确失败，不以跳过数据库测试代替成功。
单独执行 Maven 时，真实金仓测试默认不运行；完整验证应使用上述统一脚本。

手动检查：登录、查询、切换台站、新增 / 编辑传感器、确认删除、修改后刷新、未保存更改提示、退出后不能继续访问业务页面。
