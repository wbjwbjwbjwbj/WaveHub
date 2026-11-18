# 开发说明

## 模块结构

前端在 `src/views` 中增加业务页面，在 `src/router` 中声明路由，在 `src/api` 中管理请求；共享类型放入 `src/types`，共享状态放入 `src/stores`。

后端位于 `com.wavehub`，按 `modules/<业务模块>/controller`、`service`、`dto`、`entity`、`mapper` 组织代码。仅在有实际功能时创建实体和 Mapper；金仓 Mapper 使用 `@Mapper` 标注，并在需要时限定 `@Profile("kingbase")`。公共响应和异常放在 `common`，框架配置放在 `config`。

## 接口约定

业务接口以 `/api` 为前缀，响应格式如下。错误应同时使用正确的 HTTP 状态码，不将失败包装为 HTTP 200。

```json
{
  "code": "OK",
  "message": "操作成功",
  "data": {}
}
```

| 接口 | 访问 | 返回 |
| --- | --- | --- |
| `GET /api/system/info` | 公开 | 应用名称、版本、数据库模式、UTC 服务时间 |
| `GET /api/health` | 公开 | Actuator 健康状态，如 `{"status":"UP"}`，不使用业务响应包装 |
| `GET /api/auth/csrf` | 公开 | CSRF headerName、parameterName、token；创建会话时携带 HttpOnly Cookie |
| 其余接口 | 要求认证 | 未认证返回 401；缺少有效 CSRF 令牌的写请求返回 403 |

`databaseMode` 只表示启动模式，不表示已经执行 SQL 验证。金仓连接就绪情况应通过健康检查确认。

前端写请求先获取 `/api/auth/csrf`，保留会话 Cookie，并将 token 放入返回的 headerName 对应请求头。将来实现登录和退出时，认证状态变化后需要重新获取令牌。不要关闭 CSRF 防护。

## 后续业务约定

- 分页请求采用 `page`（从 1 开始）和 `size`（最多 200），响应字段采用 `records`、`total`、`page`、`size`。
- 时间接口值采用 ISO 8601，前端负责本地时区展示。
- Java `Long` 类型的业务 ID 对外序列化为字符串，避免 JavaScript 精度丢失。
- 后端使用 DTO 校验用户输入；新增业务接口必须实现权限和数据访问校验。
- 根据本机 KingbaseES 兼容模式选择字段类型、主键策略和 SQL；不直接复制 MySQL DDL。

## 本地工具

`.cache/` 为被 Git 忽略的本地依赖缓存目录，不属于项目交付依赖。
本机 Java 21 位于 `C:\Users\wang\.jdks\corretto-21.0.4`；只在当前终端设置 `JAVA_HOME` 即可，不必修改系统默认环境。
其他开发者只需安装 JDK 21，再按根目录 README 使用 Maven Wrapper 构建。
