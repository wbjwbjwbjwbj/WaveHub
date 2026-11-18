# WaveHub

面向管理后台 / 业务系统的前后端分离项目骨架。

- 前端：Vue 3、TypeScript、Vite、Element Plus、Vue Router、Pinia、Axios。
- 后端：Java 21、Spring Boot 4.1.2、Spring Security、MyBatis-Plus 3.5.18。
- 数据库：用户本机的人大金仓 KingbaseES，使用官方 JDBC 驱动。

## 目录

```text
WaveHub/
├── AGENTS.md
├── frontend/                 # 前端源码和 pnpm 锁文件
├── backend/                  # 后端源码、测试、Maven Wrapper
├── deploy/nginx.conf         # 同源反向代理配置示例
├── docs/development.md       # 接口及开发说明
└── README.md
```

## 环境要求

- Node.js 22.12+（22.x）或 24+；pnpm 11.25.0。
- **JDK 21 或更高版本**，`JAVA_HOME` 指向对应 JDK。本机已有 `C:\Users\wang\.jdks\corretto-21.0.4`，但默认 `JAVA_HOME` 为 Java 17，启动前需要切换。
- Maven Wrapper 自动下载 Maven 3.9.11，首次安装依赖需要网络。

## 启动前端

```powershell
cd E:\vscodeProjects\WaveHub\frontend
pnpm install --frozen-lockfile
pnpm dev
```

访问 <http://127.0.0.1:5173>。Vite 将 `/api` 代理至 `http://127.0.0.1:8080`。
需要更改后端地址时，将 `frontend/.env.example` 复制为 `.env.local` 后编辑并重启 Vite。

## 启动后端（独立模式）

在另一个 PowerShell 窗口中执行。以下使用本机已安装的 Java 21，其他机器需修改路径：

```powershell
$env:JAVA_HOME = 'C:\Users\wang\.jdks\corretto-21.0.4'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
cd E:\vscodeProjects\WaveHub\backend
.\mvnw.cmd spring-boot:run
```

默认 `standalone` 模式不连接数据库，可先验证前后端联通。工作台实时显示 API 连接结果。

- 服务信息：<http://127.0.0.1:8080/api/system/info>
- 健康检查：<http://127.0.0.1:8080/api/health>

## 接入本机人大金仓

先确认实际数据库版本、兼容模式、端口、库名、schema 和开发账户。框架不创建或删除数据库、不自动执行 SQL。

```powershell
$env:SPRING_PROFILES_ACTIVE = 'kingbase'
$env:DB_URL = 'jdbc:kingbase8://127.0.0.1:<实际端口>/<实际数据库名>'
$env:DB_USERNAME = '<开发数据库用户>'
$env:DB_PASSWORD = '<开发数据库密码>'
cd E:\vscodeProjects\WaveHub\backend
.\mvnw.cmd spring-boot:run
```

以上值均需替换。请通过 IDE 环境变量或本地安全配置填写密码，不将真实凭据写入源码。
`backend/.env.example` 只说明参数，Spring Boot **不会自动加载该文件**。

当前驱动锁定为 Maven Central 发布的 `cn.com.kingbase:kingbase8:8.6.1`，不代表已验证匹配本机服务端。
若本机数据库要求不同驱动，先确认官方兼容说明，再通过 `-Dkingbase.version=<版本>` 覆盖或修改 POM。
分页插件已配置 `DbType.KINGBASE_ES`；真实 SQL、分页、事务行为需在金仓开发库中验证。

切回独立模式：

```powershell
$env:SPRING_PROFILES_ACTIVE = 'standalone'
.\mvnw.cmd spring-boot:run
```

数据库脚本目录为 `backend/src/main/resources/db/migration/`。当前未启用 Flyway。

## 检查与构建

```powershell
cd E:\vscodeProjects\WaveHub\frontend
pnpm lint
pnpm format:check
pnpm build

cd E:\vscodeProjects\WaveHub\backend
.\mvnw.cmd clean verify
```

前端产物在 `frontend/dist/`，后端可执行 JAR 在 `backend/target/wavehub-backend-0.1.0.jar`。
后端集成测试使用独立模式，覆盖公开接口、健康检查、未认证拦截和 CSRF 防护；不代替金仓数据库测试。

## 当前范围

已提供工作台布局、路由、共享状态、HTTP 封装、统一 API 响应、异常处理、基础访问控制、CSRF 接口、金仓连接和分页配置。

登录、用户、角色、菜单及业务 CRUD 尚未实现，未提供默认账号。除公开接口外，其余请求需要认证；后续应补充 Session 登录和后端 RBAC 校验。

生产部署参考 `deploy/nginx.conf`，部署前设置数据库模式和凭据，HTTPS 环境设置 `SESSION_COOKIE_SECURE=true`。
后端默认仅监听 `127.0.0.1`；容器部署可通过 `SERVER_ADDRESS=0.0.0.0` 调整。
