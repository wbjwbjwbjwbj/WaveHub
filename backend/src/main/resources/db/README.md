# WaveHub 原型数据库脚本

当前脚本是依据 `docs/database-design.md` v0.3 编写的可修改基线，不是最终业务模型。
面向人大金仓 KingbaseES，使用 BIGINT、NUMERIC、TEXT、带时区时间戳、外键、CHECK 和 COMMENT ON 等 SQL。
事务语句及具体类型仍需在本机 KingbaseES 实际版本和兼容模式中验证；离线解析不能替代该验证。

## 文件及执行顺序

| 顺序 | 文件 | 用途 |
| --- | --- | --- |
| 1 | `migration/V001__create_core_tables.sql` | 在空 schema 中创建 11 张核心表、约束和索引 |
| 2（可选） | `seed/prototype_seed.sql` | 导入仅用于原型演示的虚构基础数据 |

脚本不创建数据库或 schema，不包含 DROP / TRUNCATE / DELETE，不自动执行，不启用 Flyway。

1. 在金仓管理工具中连接专用开发数据库，确认实际版本、兼容模式和目标 schema。连接用户需要相应建表权限。
2. 通过 `SELECT version();`、`SELECT current_schema();` 核对所连接的实例和当前 schema；如目标模式不支持这些查询，使用管理工具查看。
3. 以 UTF-8 打开并完整执行建表文件。脚本使用显式 `BEGIN` / `COMMIT`，执行工具应遇错停止；报错后先回滚并检查，不继续执行后续脚本。
4. 确认建表成功后，按需执行示例数据文件。示例只供开发演示，不导入生产库。
5. 在已经成功建表的情况下启用后端 `kingbase` 模式，并验证健康检查；该模式仍不会自动加载这些脚本。

## 结构与字段约定

- 传感器、阵元、采集设备是同一个实体：`sensor`；一个 `station` 对应多个 `sensor`。
- 所有主键为应用分配的 BIGINT；后续实体可使用 MyBatis-Plus `ASSIGN_ID`，API 对外将 ID 转为字符串。直接手工插入时必须提供不冲突的 ID。
- 样例 ID：台站 1001–1002，阵元 1101–1103 / 1201–1203，事件 2001，参数方案 3001–3003；应用不应假定这些 ID 永久存在。
- 位置暂按 WGS84 经纬度；高程以 m 为单位。事件 `depth_m` 暂约定相对参考地表向下为正，可为空，参考面需进一步讨论。
- 测向角暂约定从真北顺时针、指向信号来源；接入算法时必须核对并转换，不能直接把另一种方向定义写入。
- 时间使用 `TIMESTAMP(6) WITH TIME ZONE`，应用交换时使用带偏移或 UTC 的 ISO 8601 时间。精度暂为微秒；有更高精度需求时需再调整。
- 初始数据中的带时区时间使用 `CAST('2026-11-18 00:00:00+00:00' AS TIMESTAMP WITH TIME ZONE)` 显式转换。本机执行反馈表明 `TIMESTAMP WITH TIME ZONE '...'` 时间字面量写法在当前环境中报语法错误；该问题与建表时的字段类型声明应分开处理。
- JSON 对象 / 数组以 TEXT 保存，不在数据库中假定原生 JSON 功能；应用负责校验 JSON 格式、结构版本和必填参数。
- `updated_at` 只在 INSERT 时提供默认值，UPDATE 时由应用更新；本版不提供触发器或软删除。
- 外键保持默认的引用保护，不级联删除。删除顺序及历史保留策略在业务功能落地时明确。
- 台站编码全局唯一，传感器编码在所属台站内唯一。
- `waveform_file` 中的 `root_key` 对应应用配置的数据根目录，`relative_path` 暂限制为 512 字符并在根目录内唯一。文件未变化时复用索引，变化时重新确认元数据及历史快照。
- 文件格式用 `format_code` 标识，不在数据库枚举中锁死支持格式；解析器支持范围由应用判断。
- `waveform_trace` 的起始时刻、采样率、样本数或传感器映射允许暂缺，但计算前由应用检查完整性。
- 选段采用从 0 开始的 `[start_sample, end_sample)`。应用还须检查右端不超过实际样本数，以及任务输入的传感器与波形映射一致。
- 计算参数和输入坐标等信息保留快照；原型参数方案为 `DRAFT`、`READY` 或 `ARCHIVED`，只有应用校验过的 READY 方案可提交执行。
- 任务类型先约定 `FILTER`、`TIME_FREQUENCY`、`WINPMCC`、`LOCALIZATION`、`ARRIVAL_PREDICTION`；状态为 `CREATED`、`RUNNING`、`SUCCEEDED`、`FAILED`、`CANCELLED`。
- 应用需校验任务与方案类型匹配、结果与任务类型匹配、任务状态流转、选段边界及定位输入是否足够且属于同一待分析信号。外键不能代替这些业务校验。
- 波形原始数据和大型计算产物存文件，`metadata_json`、`artifacts_json` 等只保存描述、路径和摘要，不保存全部采样点或图像。

## 初始示例数据

`prototype_seed.sql` 导入以下虚构记录：

- 2 个示例台站，每站 3 个阵元，共 6 个传感器；型号、序列号、标称采样率等未知参数留空。
- 1 个明确标记为 DEMO 的虚构事件。
- 3 个参数草稿：滤波、时频、WinPMCC。参数内容为 `{}`，不把尚未确认的算法参数伪装成可用配置。

波形文件、波形段、分析任务和各类结果表初始为空。没有真实波形文件就不登记伪造路径，也不填入虚构的成功运行结果。

两份脚本均按一次性执行设计，不做重复执行时的自动覆盖。重复执行会因表、ID 或编码已存在而报错。不要在有业务数据的库中反复运行初始化脚本。

导入示例后可核对：

```sql
SELECT 'station' AS table_name, COUNT(*) AS row_count FROM station
UNION ALL SELECT 'sensor', COUNT(*) FROM sensor
UNION ALL SELECT 'source_event', COUNT(*) FROM source_event
UNION ALL SELECT 'analysis_preset', COUNT(*) FROM analysis_preset;
```

预期计数依次为 2、6、1、3。也可按台站查询 `sensor`，确认两组阵元关系。

## 后续如何修改

- 尚未在共享环境使用时，可以直接修改 V001 和示例数据文件，在新的空开发 schema 中重新验证。
- 已应用到需要保留数据的环境后，保留已执行脚本，再增加 `migration/V002__具体变更.sql`、`V003__具体变更.sql` 等增量脚本，使用 ALTER TABLE 等语句调整结构并记录执行版本。
- 示例数据新增、修改应单独维护；生产基础数据如确有需要，再以独立版本脚本管理，不依赖 DEMO 数据。
- 当前未实现登录、权限和 3 张候选表。WinPMCC 完整参数、传播模型、通道映射等仍待需求讨论。

## 本次验证范围

已使用 SQLGlot 的 PostgreSQL 方言离线解析两份脚本，并核对 11 张表、外键目标、示例数据列与必填字段、ID 唯一性、台站和阵元引用、JSON 内容及示例方案 DRAFT 状态。
该检查只验证脚本语法和部分结构一致性，未连接本机 KingbaseES，未执行建表或导入，也不代表其具体版本和兼容模式已通过验证。

参考：[金仓数据类型说明](https://bbs.kingbase.com.cn/kingbase-doc/v8/development/develop-transfer/kes-vs-mysql/kes-vs-mysql-1.html)、[金仓表管理说明](https://help.kingbase.com.cn/v8.6.7.24/admin/general/administrator-guide/13-managing-tables.html)。
