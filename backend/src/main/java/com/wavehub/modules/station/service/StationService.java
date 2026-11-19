package com.wavehub.modules.station.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wavehub.common.exception.BusinessException;
import com.wavehub.modules.station.dto.*;
import com.wavehub.modules.station.entity.*;
import com.wavehub.modules.station.mapper.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@Profile("kingbase")
@Transactional
public class StationService {
    private final StationMapper stations;
    private final SensorMapper sensors;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public StationService(StationMapper stations, SensorMapper sensors, JdbcTemplate jdbc, ObjectMapper json) {
        this.stations = stations; this.sensors = sensors; this.jdbc = jdbc; this.json = json;
    }

    private static String like(String text) {
        return "%" + text.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    public Map<String, Object> list(String keyword, Boolean enabled, int page, int size) {
        QueryWrapper<Station> query = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) query.and(w -> w
                .apply("code LIKE {0} ESCAPE '!'", like(keyword)).or()
                .apply("name LIKE {0} ESCAPE '!'", like(keyword)));
        if (enabled != null) query.eq("enabled", enabled ? 1 : 0);
        query.orderByDesc("created_at", "id");
        Page<Station> result = stations.selectPage(new Page<>(page, size), query);
        Map<Long, Long> counts = new HashMap<>();
        if (!result.getRecords().isEmpty()) {
            Object[] ids = result.getRecords().stream().map(s -> s.id).toArray();
            String placeholders = String.join(",", Collections.nCopies(ids.length, "?"));
            jdbc.query("SELECT station_id, COUNT(*) FROM sensor WHERE station_id IN (" + placeholders
                    + ") GROUP BY station_id", rs -> { counts.put(rs.getLong(1), rs.getLong(2)); }, ids);
        }
        return Map.of("records", result.getRecords().stream().map(s -> stationView(s, counts.getOrDefault(s.id, 0L))).toList(),
                "total", result.getTotal(), "page", page, "size", size);
    }

    public Station requireStation(long id) {
        Station station = stations.selectById(id);
        if (station == null) throw new BusinessException(404, "STATION_NOT_FOUND", "台站不存在");
        return station;
    }

    public Map<String, Object> detail(long id) {
        return stationView(requireStation(id), sensors.selectCount(new QueryWrapper<Sensor>().eq("station_id", id)));
    }

    public Map<String, Object> saveStation(Long id, StationRequest request) {
        Station station = id == null ? new Station() : requireStation(id);
        QueryWrapper<Station> duplicates = new QueryWrapper<Station>().eq("code", request.code());
        if (id != null) duplicates.ne("id", id);
        if (stations.selectCount(duplicates) > 0) throw new BusinessException(409, "STATION_CODE_EXISTS", "台站编码已存在");
        station.code = request.code(); station.name = request.name().trim();
        station.longitude = request.longitude(); station.latitude = request.latitude();
        station.elevationM = request.elevationM(); station.enabled = request.enabled() ? 1 : 0;
        station.description = request.description(); station.updatedAt = OffsetDateTime.now();
        if (id == null) { station.createdAt = station.updatedAt; stations.insert(station); }
        else stations.updateById(station);
        return detail(station.id);
    }

    public void deleteStation(long id) {
        requireStation(id);
        if (sensors.selectCount(new QueryWrapper<Sensor>().eq("station_id", id)) > 0)
            throw new BusinessException(409, "STATION_HAS_SENSORS", "台站仍有传感器，请先处理关联传感器或停用台站");
        stations.deleteById(id);
    }

    private Sensor requireSensor(long stationId, long id) {
        requireStation(stationId);
        Sensor sensor = sensors.selectOne(new QueryWrapper<Sensor>().eq("id", id).eq("station_id", stationId));
        if (sensor == null) throw new BusinessException(404, "SENSOR_NOT_FOUND", "传感器不存在或不属于此台站");
        return sensor;
    }

    public Map<String, Object> listSensors(long stationId, String keyword, Boolean enabled, int page, int size) {
        requireStation(stationId);
        QueryWrapper<Sensor> query = new QueryWrapper<Sensor>().eq("station_id", stationId);
        if (keyword != null && !keyword.isBlank()) query.and(w -> w
                .apply("code LIKE {0} ESCAPE '!'", like(keyword)).or()
                .apply("name LIKE {0} ESCAPE '!'", like(keyword)).or()
                .apply("serial_number LIKE {0} ESCAPE '!'", like(keyword)));
        if (enabled != null) query.eq("enabled", enabled ? 1 : 0);
        query.orderByDesc("created_at", "id");
        Page<Sensor> result = sensors.selectPage(new Page<>(page, size), query);
        return Map.of("records", result.getRecords().stream().map(this::sensorView).toList(),
                "total", result.getTotal(), "page", page, "size", size);
    }

    public Map<String, Object> sensorDetail(long stationId, long id) { return sensorView(requireSensor(stationId, id)); }

    public Map<String, Object> saveSensor(long stationId, Long id, SensorRequest request) {
        requireStation(stationId);
        Sensor sensor = id == null ? new Sensor() : requireSensor(stationId, id);
        QueryWrapper<Sensor> duplicates = new QueryWrapper<Sensor>().eq("station_id", stationId).eq("code", request.code());
        if (id != null) duplicates.ne("id", id);
        if (sensors.selectCount(duplicates) > 0) throw new BusinessException(409, "SENSOR_CODE_EXISTS", "此台站下的传感器编码已存在");
        String parameters = json.writeValueAsString(request.parameters() == null ? Map.of() : request.parameters());
        if (parameters.getBytes(StandardCharsets.UTF_8).length > 16384)
            throw new BusinessException(400, "VALIDATION_ERROR", "扩展参数不能超过 16 KiB");
        sensor.stationId = stationId; sensor.code = request.code(); sensor.name = request.name().trim();
        sensor.longitude = request.longitude(); sensor.latitude = request.latitude(); sensor.elevationM = request.elevationM();
        sensor.sensorType = request.sensorType(); sensor.model = request.model(); sensor.serialNumber = request.serialNumber();
        sensor.nominalSampleRateHz = request.nominalSampleRateHz(); sensor.parametersJson = parameters;
        sensor.parameterSchemaVersion = 1; sensor.enabled = request.enabled() ? 1 : 0;
        sensor.description = request.description(); sensor.updatedAt = OffsetDateTime.now();
        if (id == null) { sensor.createdAt = sensor.updatedAt; sensors.insert(sensor); }
        else sensors.updateById(sensor);
        return sensorView(sensor);
    }

    public void deleteSensor(long stationId, long id) {
        requireSensor(stationId, id);
        Long references = jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM waveform_trace WHERE sensor_id = ?)"
                + " + (SELECT COUNT(*) FROM analysis_task_input WHERE sensor_id = ?)", Long.class, id, id);
        if (references != null && references > 0)
            throw new BusinessException(409, "SENSOR_IN_USE", "传感器已被波形或任务使用，请改为停用");
        sensors.deleteById(id);
    }

    private Map<String, Object> stationView(Station station, long count) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", station.id.toString()); view.put("code", station.code); view.put("name", station.name);
        view.put("longitude", station.longitude); view.put("latitude", station.latitude); view.put("elevationM", station.elevationM);
        view.put("enabled", station.enabled == 1); view.put("description", station.description); view.put("sensorCount", count);
        view.put("createdAt", station.createdAt); view.put("updatedAt", station.updatedAt);
        return view;
    }

    private Map<String, Object> sensorView(Sensor sensor) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", sensor.id.toString()); view.put("stationId", sensor.stationId.toString());
        view.put("code", sensor.code); view.put("name", sensor.name); view.put("longitude", sensor.longitude);
        view.put("latitude", sensor.latitude); view.put("elevationM", sensor.elevationM); view.put("sensorType", sensor.sensorType);
        view.put("model", sensor.model); view.put("serialNumber", sensor.serialNumber); view.put("nominalSampleRateHz", sensor.nominalSampleRateHz);
        view.put("parameters", json.readValue(sensor.parametersJson, Map.class)); view.put("enabled", sensor.enabled == 1);
        view.put("description", sensor.description); view.put("createdAt", sensor.createdAt); view.put("updatedAt", sensor.updatedAt);
        return view;
    }
}
