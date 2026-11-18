package com.wavehub.modules.auth.controller;

import com.wavehub.common.api.ApiResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {
    @GetMapping("/api/auth/csrf")
    public ApiResponse<CsrfInfo> csrf(CsrfToken token) {
        return ApiResponse.success(new CsrfInfo(token.getHeaderName(), token.getParameterName(), token.getToken()));
    }

    public record CsrfInfo(String headerName, String parameterName, String token) {}
}
