package com.wavehub.modules.station.controller;

import com.wavehub.common.api.ApiResponse;
import com.wavehub.common.exception.BusinessException;
import com.wavehub.modules.station.dto.*;
import com.wavehub.modules.station.service.StationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stations")
public class StationController {
    private final ObjectProvider<StationService> provider;
    public StationController(ObjectProvider<StationService> provider) { this.provider = provider; }
    private StationService service() {
        StationService service = provider.getIfAvailable();
        if (service == null) throw new BusinessException(503, "DATABASE_UNAVAILABLE", "数据库未启用");
        return service;
    }
    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) @Size(max = 128) String keyword,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return ApiResponse.success(service().list(keyword, enabled, page, size));
    }
    @GetMapping("/{stationId}")
    public ApiResponse<?> detail(@PathVariable @Positive long stationId) { return ApiResponse.success(service().detail(stationId)); }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<?> create(@Valid @RequestBody StationRequest request) { return ApiResponse.success(service().saveStation(null, request)); }
    @PutMapping("/{stationId}")
    public ApiResponse<?> update(@PathVariable @Positive long stationId, @Valid @RequestBody StationRequest request) {
        return ApiResponse.success(service().saveStation(stationId, request));
    }
    @DeleteMapping("/{stationId}")
    public ApiResponse<?> delete(@PathVariable @Positive long stationId) { service().deleteStation(stationId); return ApiResponse.success(null); }
    @GetMapping("/{stationId}/sensors")
    public ApiResponse<?> sensors(@PathVariable @Positive long stationId,
            @RequestParam(required = false) @Size(max = 128) String keyword, @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "1") @Min(1) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return ApiResponse.success(service().listSensors(stationId, keyword, enabled, page, size));
    }
    @GetMapping("/{stationId}/sensors/{sensorId}")
    public ApiResponse<?> sensor(@PathVariable @Positive long stationId, @PathVariable @Positive long sensorId) {
        return ApiResponse.success(service().sensorDetail(stationId, sensorId));
    }
    @PostMapping("/{stationId}/sensors")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<?> createSensor(@PathVariable @Positive long stationId, @Valid @RequestBody SensorRequest request) {
        return ApiResponse.success(service().saveSensor(stationId, null, request));
    }
    @PutMapping("/{stationId}/sensors/{sensorId}")
    public ApiResponse<?> updateSensor(@PathVariable @Positive long stationId, @PathVariable @Positive long sensorId,
            @Valid @RequestBody SensorRequest request) { return ApiResponse.success(service().saveSensor(stationId, sensorId, request)); }
    @DeleteMapping("/{stationId}/sensors/{sensorId}")
    public ApiResponse<?> deleteSensor(@PathVariable @Positive long stationId, @PathVariable @Positive long sensorId) {
        service().deleteSensor(stationId, sensorId); return ApiResponse.success(null);
    }
}
