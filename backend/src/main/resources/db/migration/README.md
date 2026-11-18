# 数据库脚本

本目录包含手动执行的版本化建表脚本。项目不会自动执行 SQL，尚未启用 Flyway。

当前结构：`V001__create_core_tables.sql`，包含 11 张核心业务表、主外键、约束、索引和注释。

可选示例数据在相邻目录 `../seed/prototype_seed.sql`，不属于生产迁移。
执行顺序、字段约定和后续修改方法见 [`../README.md`](../README.md)。

迁移工具的支持范围及授权经过确认后再接入，不将 PostgreSQL 协议兼容视为 Flyway 已适配。
