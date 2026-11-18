# 数据库脚本

此目录保留给后续版本化 SQL。当前不创建业务表、不自动执行 SQL，也未启用 Flyway。

接入前确认本机 KingbaseES 版本、兼容模式、schema、字符集及 JDBC 驱动匹配情况。
脚本按 `V001__description.sql` 命名，在专用开发库验证后手动执行并记录版本。
迁移工具的支持范围及授权经过确认后再接入，不将 PostgreSQL 协议兼容视为 Flyway 已适配。
