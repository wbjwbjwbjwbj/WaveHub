-- WaveHub 原型结构 v0.3；人大金仓 KingbaseES，兼容模式及具体版本须在开发库确认。
-- UTF-8；在选定的空 schema 中执行一次。无建库、删表、清表或自动覆盖操作。
-- ID 由应用分配（后续可使用 MyBatis-Plus ASSIGN_ID），本脚本不创建序列。
-- 坐标暂按 WGS84 经度/纬度，长度 m、速度 m/s、频率 Hz；均为原型约定。
-- 时间使用带时区时间戳；参数对象以 JSON 文本保存，由应用校验。
BEGIN;

CREATE TABLE station (
    id BIGINT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    longitude NUMERIC(11, 7) NOT NULL,
    latitude NUMERIC(10, 7) NOT NULL,
    elevation_m NUMERIC(12, 3),
    enabled SMALLINT NOT NULL DEFAULT 1,
    description TEXT,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_station_code UNIQUE (code),
    CONSTRAINT ck_station_lon CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_station_lat CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_station_enabled CHECK (enabled IN (0, 1))
);

CREATE TABLE sensor (
    id BIGINT PRIMARY KEY,
    station_id BIGINT NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    sensor_type VARCHAR(64),
    model VARCHAR(128),
    serial_number VARCHAR(128),
    longitude NUMERIC(11, 7) NOT NULL,
    latitude NUMERIC(10, 7) NOT NULL,
    elevation_m NUMERIC(12, 3),
    nominal_sample_rate_hz NUMERIC(20, 8),
    parameter_schema_version INTEGER NOT NULL DEFAULT 1,
    parameters_json TEXT NOT NULL DEFAULT '{}',
    enabled SMALLINT NOT NULL DEFAULT 1,
    description TEXT,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sensor_station FOREIGN KEY (station_id) REFERENCES station (id),
    CONSTRAINT uk_sensor_station_code UNIQUE (station_id, code),
    CONSTRAINT ck_sensor_lon CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_sensor_lat CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_sensor_rate CHECK (nominal_sample_rate_hz > 0),
    CONSTRAINT ck_sensor_schema CHECK (parameter_schema_version > 0),
    CONSTRAINT ck_sensor_enabled CHECK (enabled IN (0, 1))
);

CREATE TABLE waveform_file (
    id BIGINT PRIMARY KEY,
    root_key VARCHAR(64) NOT NULL,
    relative_path VARCHAR(512) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    format_code VARCHAR(32) NOT NULL,
    source_type VARCHAR(16) NOT NULL DEFAULT 'ORIGINAL',
    file_size_bytes BIGINT,
    file_modified_at TIMESTAMP(6) WITH TIME ZONE,
    content_sha256 VARCHAR(64),
    parser_name VARCHAR(128),
    parser_version VARCHAR(64),
    parse_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    metadata_json TEXT NOT NULL DEFAULT '{}',
    created_by_task_id BIGINT,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_waveform_file_path UNIQUE (root_key, relative_path),
    CONSTRAINT ck_waveform_file_size CHECK (file_size_bytes >= 0),
    CONSTRAINT ck_waveform_file_source CHECK (source_type IN ('ORIGINAL', 'DERIVED')),
    CONSTRAINT ck_waveform_file_status CHECK (parse_status IN
        ('PENDING', 'READY', 'NEEDS_METADATA', 'FAILED', 'MISSING', 'CHANGED'))
);

CREATE TABLE waveform_trace (
    id BIGINT PRIMARY KEY,
    file_id BIGINT NOT NULL,
    sensor_id BIGINT,
    trace_key VARCHAR(255) NOT NULL,
    segment_index INTEGER NOT NULL DEFAULT 0,
    channel_code VARCHAR(64),
    start_time TIMESTAMP(6) WITH TIME ZONE,
    sample_rate_hz NUMERIC(20, 8),
    sample_count BIGINT,
    physical_unit VARCHAR(32),
    locator_json TEXT NOT NULL DEFAULT '{}',
    metadata_json TEXT NOT NULL DEFAULT '{}',
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trace_file FOREIGN KEY (file_id) REFERENCES waveform_file (id),
    CONSTRAINT fk_trace_sensor FOREIGN KEY (sensor_id) REFERENCES sensor (id),
    CONSTRAINT uk_trace_segment UNIQUE (file_id, trace_key, segment_index),
    CONSTRAINT ck_trace_segment CHECK (segment_index >= 0),
    CONSTRAINT ck_trace_rate CHECK (sample_rate_hz > 0),
    CONSTRAINT ck_trace_count CHECK (sample_count >= 0)
);

CREATE TABLE source_event (
    id BIGINT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    origin_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    longitude NUMERIC(11, 7) NOT NULL,
    latitude NUMERIC(10, 7) NOT NULL,
    depth_m NUMERIC(12, 3),
    source_kind VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    external_reference VARCHAR(255),
    metadata_json TEXT NOT NULL DEFAULT '{}',
    description TEXT,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_source_event_code UNIQUE (code),
    CONSTRAINT ck_event_lon CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_event_lat CHECK (latitude BETWEEN -90 AND 90)
);

CREATE TABLE analysis_preset (
    id BIGINT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    analysis_type VARCHAR(32) NOT NULL,
    schema_version INTEGER NOT NULL DEFAULT 1,
    parameters_json TEXT NOT NULL DEFAULT '{}',
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    description TEXT,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_analysis_preset_code UNIQUE (code),
    CONSTRAINT ck_preset_type CHECK (analysis_type IN
        ('FILTER', 'TIME_FREQUENCY', 'WINPMCC', 'LOCALIZATION', 'ARRIVAL_PREDICTION')),
    CONSTRAINT ck_preset_schema CHECK (schema_version > 0),
    CONSTRAINT ck_preset_status CHECK (status IN ('DRAFT', 'READY', 'ARCHIVED'))
);

CREATE TABLE analysis_task (
    id BIGINT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    analysis_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'CREATED',
    preset_id BIGINT,
    source_event_id BIGINT,
    parent_task_id BIGINT,
    schema_version INTEGER NOT NULL DEFAULT 1,
    parameters_snapshot_json TEXT NOT NULL,
    algorithm_name VARCHAR(128),
    algorithm_version VARCHAR(64),
    progress_percent NUMERIC(5, 2) NOT NULL DEFAULT 0,
    started_at TIMESTAMP(6) WITH TIME ZONE,
    finished_at TIMESTAMP(6) WITH TIME ZONE,
    artifacts_json TEXT NOT NULL DEFAULT '[]',
    error_message TEXT,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_preset FOREIGN KEY (preset_id) REFERENCES analysis_preset (id),
    CONSTRAINT fk_task_event FOREIGN KEY (source_event_id) REFERENCES source_event (id),
    CONSTRAINT fk_task_parent FOREIGN KEY (parent_task_id) REFERENCES analysis_task (id),
    CONSTRAINT ck_task_type CHECK (analysis_type IN
        ('FILTER', 'TIME_FREQUENCY', 'WINPMCC', 'LOCALIZATION', 'ARRIVAL_PREDICTION')),
    CONSTRAINT ck_task_status CHECK (status IN
        ('CREATED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
    CONSTRAINT ck_task_schema CHECK (schema_version > 0),
    CONSTRAINT ck_task_progress CHECK (progress_percent BETWEEN 0 AND 100),
    CONSTRAINT ck_task_time CHECK (finished_at >= started_at),
    CONSTRAINT ck_task_parent CHECK (parent_task_id <> id)
);

-- 派生文件与任务存在前后依赖，在任务表创建后补充外键。
ALTER TABLE waveform_file ADD CONSTRAINT fk_file_created_task
    FOREIGN KEY (created_by_task_id) REFERENCES analysis_task (id);

CREATE TABLE analysis_task_input (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    trace_id BIGINT NOT NULL,
    sensor_id BIGINT NOT NULL,
    start_sample BIGINT NOT NULL,
    end_sample BIGINT NOT NULL,
    metadata_snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_input_task FOREIGN KEY (task_id) REFERENCES analysis_task (id),
    CONSTRAINT fk_task_input_trace FOREIGN KEY (trace_id) REFERENCES waveform_trace (id),
    CONSTRAINT fk_task_input_sensor FOREIGN KEY (sensor_id) REFERENCES sensor (id),
    CONSTRAINT uk_task_input_range UNIQUE (task_id, trace_id, start_sample, end_sample),
    CONSTRAINT ck_task_input_range CHECK (start_sample >= 0 AND end_sample > start_sample)
);

CREATE TABLE direction_result (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    result_index INTEGER NOT NULL,
    signal_start TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    signal_end TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    representative_time TIMESTAMP(6) WITH TIME ZONE,
    azimuth_deg NUMERIC(10, 6) NOT NULL,
    apparent_velocity_mps NUMERIC(20, 8),
    reference_longitude NUMERIC(11, 7) NOT NULL,
    reference_latitude NUMERIC(10, 7) NOT NULL,
    reference_elevation_m NUMERIC(12, 3),
    reference_snapshot_json TEXT NOT NULL,
    metrics_json TEXT NOT NULL DEFAULT '{}',
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_direction_task FOREIGN KEY (task_id) REFERENCES analysis_task (id),
    CONSTRAINT uk_direction_task_index UNIQUE (task_id, result_index),
    CONSTRAINT ck_direction_index CHECK (result_index >= 0),
    CONSTRAINT ck_direction_time CHECK (signal_end > signal_start),
    CONSTRAINT ck_direction_repr CHECK
        (representative_time >= signal_start AND representative_time <= signal_end),
    CONSTRAINT ck_direction_azimuth CHECK (azimuth_deg >= 0 AND azimuth_deg < 360),
    CONSTRAINT ck_direction_velocity CHECK (apparent_velocity_mps > 0),
    CONSTRAINT ck_direction_lon CHECK (reference_longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_direction_lat CHECK (reference_latitude BETWEEN -90 AND 90)
);

CREATE TABLE localization_result (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    source_event_id BIGINT,
    candidate_index INTEGER NOT NULL DEFAULT 0,
    longitude NUMERIC(11, 7) NOT NULL,
    latitude NUMERIC(10, 7) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'CANDIDATE',
    metrics_json TEXT NOT NULL DEFAULT '{}',
    description TEXT,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_localization_task FOREIGN KEY (task_id) REFERENCES analysis_task (id),
    CONSTRAINT fk_localization_event FOREIGN KEY (source_event_id) REFERENCES source_event (id),
    CONSTRAINT uk_localization_candidate UNIQUE (task_id, candidate_index),
    CONSTRAINT ck_localization_index CHECK (candidate_index >= 0),
    CONSTRAINT ck_localization_lon CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_localization_lat CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_localization_status CHECK (status IN ('CANDIDATE', 'CONFIRMED', 'REJECTED'))
);

CREATE TABLE localization_input (
    id BIGINT PRIMARY KEY,
    localization_result_id BIGINT NOT NULL,
    direction_result_id BIGINT NOT NULL,
    input_weight NUMERIC(20, 8) NOT NULL DEFAULT 1,
    direction_snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loc_input_result FOREIGN KEY (localization_result_id) REFERENCES localization_result (id),
    CONSTRAINT fk_loc_input_direction FOREIGN KEY (direction_result_id) REFERENCES direction_result (id),
    CONSTRAINT uk_localization_input UNIQUE (localization_result_id, direction_result_id),
    CONSTRAINT ck_localization_weight CHECK (input_weight > 0)
);

-- 唯一约束已包含按台站查询阵元、按文件查询波形段等前缀索引。
CREATE INDEX idx_file_status ON waveform_file (parse_status);
CREATE INDEX idx_file_created_task ON waveform_file (created_by_task_id);
CREATE INDEX idx_trace_sensor_time ON waveform_trace (sensor_id, start_time);
CREATE INDEX idx_event_time ON source_event (origin_time);
CREATE INDEX idx_preset_type_status ON analysis_preset (analysis_type, status);
CREATE INDEX idx_task_status_created ON analysis_task (status, created_at);
CREATE INDEX idx_task_preset ON analysis_task (preset_id);
CREATE INDEX idx_task_event ON analysis_task (source_event_id);
CREATE INDEX idx_task_parent ON analysis_task (parent_task_id);
CREATE INDEX idx_input_trace ON analysis_task_input (trace_id);
CREATE INDEX idx_input_sensor ON analysis_task_input (sensor_id);
CREATE INDEX idx_direction_time ON direction_result (signal_start, signal_end);
CREATE INDEX idx_localization_event ON localization_result (source_event_id);
CREATE INDEX idx_loc_input_direction ON localization_input (direction_result_id);

COMMENT ON TABLE station IS '台站，一站包含多个传感器';
COMMENT ON TABLE sensor IS '传感器即阵元和波形采集设备，统一管理';
COMMENT ON TABLE waveform_file IS '后端本地原始或派生波形文件索引，不保存采样点';
COMMENT ON TABLE waveform_trace IS '文件中的通道连续波形段，可暂缺时刻或传感器映射';
COMMENT ON TABLE source_event IS '作为到达推算依据的已知事件，与定位计算结果分开保存';
COMMENT ON TABLE analysis_preset IS '可复用参数方案，DRAFT 方案不能直接作为已验证配置执行';
COMMENT ON TABLE analysis_task IS '分析执行记录，保留执行时参数快照';
COMMENT ON TABLE analysis_task_input IS '执行时实际采用的阵元、波形段和选段区间';
COMMENT ON TABLE direction_result IS '信号检测及测向摘要，详细网格存产物文件';
COMMENT ON TABLE localization_result IS '交叉定位的候选或确认结果';
COMMENT ON TABLE localization_input IS '定位结果与所采用的测向结果之间的关联';
COMMENT ON COLUMN sensor.parameters_json IS 'JSON 对象文本；厂家参数结构暂定，应用负责校验';
COMMENT ON COLUMN waveform_file.root_key IS '应用配置中的可读数据根目录标识，不直接接受任意路径';
COMMENT ON COLUMN waveform_trace.start_time IS '缺失绝对起始时刻时允许为空，计算前按需补齐';
COMMENT ON COLUMN source_event.depth_m IS '原型约定相对参考地表向下为正、向上为负，参考面待讨论';
COMMENT ON COLUMN analysis_task.parameters_snapshot_json IS '完整执行参数快照，不能依赖方案当前值回放历史';
COMMENT ON COLUMN analysis_task_input.start_sample IS '从 0 开始，选段左端包含';
COMMENT ON COLUMN analysis_task_input.end_sample IS '选段右端不包含，应用检查不超过波形段 sample_count';
COMMENT ON COLUMN direction_result.azimuth_deg IS '原型约定：从真北顺时针指向信号来源，算法接入时须转换确认';
COMMENT ON COLUMN direction_result.reference_snapshot_json IS '组合参考位置及坐标约定快照，不能用单个台站代替跨站输入';

COMMIT;
