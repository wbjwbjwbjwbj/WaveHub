# 离线数据处理客户端

基于 Vue 3、Vue Router 和 Element Plus，按照 platform-apis.openapi.yaml 0.2.4 中的 `/client` 接口实现。范围为离线客户端，不含数据管理平台的波形模块。

## 启动

```powershell
npm install
npm run dev
```

如默认端口已被占用：

```powershell
npm run dev -- --port 5181 --strictPort
```

访问终端输出的地址。当前预览使用 http://127.0.0.1:5181 。

## 页面

- `/devices`：设备列表、名称和类型筛选、分页、新增、编辑、删除、IPv4 校验。
- `/models`：模型列表、ZIP 导入、信息编辑、删除、ZIP 下载。
- `/tasks`：任务创建和维护，绑定一个模型及多个设备，展示任务状态。
- `/runs`：任务启动、停止、动态参数配置，每 10 秒刷新当前列表。

运行中禁止编辑、删除和修改参数；仅 created/stopped 可启动，仅 running 可停止。任务参数支持 string、number、integer、boolean、select，校验必填、类型、范围和选项。创建时间按浏览器本地时区展示。

## 模式与接口联调

默认是明确标识的本地演示模式。演示数据保存在浏览器 localStorage，修改后刷新仍保留。演示任务启停只改变本地状态，不执行算法；演示模型导入只保存文件元信息，不读取、解压或存储 ZIP 内容，因此禁用演示模型导出。初始数据含两个模型、三台设备、两个任务。浏览器验证时另创建了“联调验证任务”。

将 `.env.example` 复制为 `.env.local`，按后端地址配置：

```dotenv
VITE_DATA_MODE=api
VITE_API_BASE_URL=/api/v1
VITE_API_PROXY_TARGET=http://localhost:8080
```

修改环境变量后重启 Vite。右上角模式选择器可切换模式，其浏览器保存值优先于环境变量。真实接口失败时显示错误，不会回退到演示数据。

开发环境 `/api` 请求代理到 VITE_API_PROXY_TARGET，保持原始路径；生产环境需由部署服务反向代理 `/api`，或设置绝对 VITE_API_BASE_URL 并由后端开放对应 CORS。使用 history 路由，生产服务需将前端路由回退到 index.html。

接口封装位于 `src/services/api.js`，统一处理错误、分页、超时、下载文件名和 multipart。模型上传字段为 file/name/type/description，不手动设置 multipart Content-Type。下载优先采用 Content-Disposition 的 filename*，跨域需服务端暴露该响应头。设备和模型选择器自动读取所有分页数据。

认证方案在接口草案中尚未约定，当前未添加登录或令牌。ZIP 校验与解压限制、模型实际执行、资源引用冲突、任务状态持久化和权限由后端处理；前端不代替服务端校验。模型参数从 `/tasks/{taskId}/parameters` 动态加载。

## 验证

```powershell
npm run build
npx eslint src vite.config.js tests
node --test tests/domain.test.js
```

已验证状态机约束和参数校验，并通过浏览器检查多设备任务创建、启动、停止、动态参数保存。尚未连接真实后端进行联调。Element Plus 完整引入会产生构建包大小提示，后续可按需加载组件优化。
