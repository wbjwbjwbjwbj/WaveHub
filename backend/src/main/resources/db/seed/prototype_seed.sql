-- 仅供空开发库的可选原型示例数据，全部为虚构数据，不是实测参数或算法推荐值。
-- 先执行 migration/V001__create_core_tables.sql。本脚本不自动运行，也不覆盖已有数据。
-- 一次性导入：固定 ID / 编码冲突会报错；应停止并回滚，不要清表或忽略错误后继续。
-- 无波形文件、运行任务、测向和定位结果，避免将不存在的数据伪装成可用数据。
BEGIN;

INSERT INTO station (id, code, name, longitude, latitude, elevation_m, description)
VALUES (1001, 'DEMO_STA_A', '示例台站 A', 116.0000000, 40.0000000, 50.000,
        '虚构原型数据，仅用于台站与传感器管理界面演示。');
INSERT INTO station (id, code, name, longitude, latitude, elevation_m, description)
VALUES (1002, 'DEMO_STA_B', '示例台站 B', 116.1000000, 40.1000000, 60.000,
        '虚构原型数据，仅用于跨站组合选择界面演示。');

INSERT INTO sensor (id, station_id, code, name, longitude, latitude, elevation_m, description)
VALUES (1101, 1001, 'A01', '台站 A 传感器 1', 116.0000000, 40.0000000, 50.000, '虚构坐标，采集参数待实机确认。');
INSERT INTO sensor (id, station_id, code, name, longitude, latitude, elevation_m, description)
VALUES (1102, 1001, 'A02', '台站 A 传感器 2', 116.0005000, 40.0000000, 50.000, '虚构坐标，采集参数待实机确认。');
INSERT INTO sensor (id, station_id, code, name, longitude, latitude, elevation_m, description)
VALUES (1103, 1001, 'A03', '台站 A 传感器 3', 116.0000000, 40.0005000, 50.000, '虚构坐标，采集参数待实机确认。');
INSERT INTO sensor (id, station_id, code, name, longitude, latitude, elevation_m, description)
VALUES (1201, 1002, 'B01', '台站 B 传感器 1', 116.1000000, 40.1000000, 60.000, '虚构坐标，采集参数待实机确认。');
INSERT INTO sensor (id, station_id, code, name, longitude, latitude, elevation_m, description)
VALUES (1202, 1002, 'B02', '台站 B 传感器 2', 116.1005000, 40.1000000, 60.000, '虚构坐标，采集参数待实机确认。');
INSERT INTO sensor (id, station_id, code, name, longitude, latitude, elevation_m, description)
VALUES (1203, 1002, 'B03', '台站 B 传感器 3', 116.1000000, 40.1005000, 60.000, '虚构坐标，采集参数待实机确认。');

INSERT INTO source_event
    (id, code, name, origin_time, longitude, latitude, source_kind, description)
VALUES (2001, 'DEMO_EVENT_001', '虚构示例事件',
        CAST('2026-11-18 00:00:00+00:00' AS TIMESTAMP WITH TIME ZONE),
        116.0500000, 40.0500000, 'DEMO',
        '仅供事件录入与地图展示演示，不是真实事件，不用于验证到达模型。');

INSERT INTO analysis_preset
    (id, code, name, analysis_type, schema_version, parameters_json, status, description)
VALUES (3001, 'DEMO_FILTER_DRAFT', '滤波参数草稿', 'FILTER', 1,
        '{}', 'DRAFT', '待讨论滤波器类型、带宽、阶数和单位后补充；当前不可执行。');
INSERT INTO analysis_preset
    (id, code, name, analysis_type, schema_version, parameters_json, status, description)
VALUES (3002, 'DEMO_FFT_DRAFT', '时频参数草稿', 'TIME_FREQUENCY', 1,
        '{}', 'DRAFT', '待讨论窗口类型、窗口长度、重叠比例等参数后补充；当前不可执行。');
INSERT INTO analysis_preset
    (id, code, name, analysis_type, schema_version, parameters_json, status, description)
VALUES (3003, 'DEMO_WINPMCC_DRAFT', 'WinPMCC 参数草稿', 'WINPMCC', 1,
        '{}', 'DRAFT', '完整参数须与实际 WinPMCC 接口核对，不提供臆造的默认值；当前不可执行。');

COMMIT;
