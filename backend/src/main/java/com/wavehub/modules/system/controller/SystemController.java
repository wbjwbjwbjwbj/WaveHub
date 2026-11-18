package com.wavehub.modules.system.controller;

import com.wavehub.common.api.ApiResponse;
import com.wavehub.modules.system.dto.SystemInfo;
import com.wavehub.modules.system.service.SystemInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {
    private final SystemInfoService service;

    public SystemController(SystemInfoService service) {
        this.service = service;
    }

    @GetMapping("/info")
    public ApiResponse<SystemInfo> info() {
        return ApiResponse.success(service.getInfo());
    }
}
