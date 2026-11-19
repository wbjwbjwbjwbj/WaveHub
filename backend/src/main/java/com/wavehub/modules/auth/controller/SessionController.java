package com.wavehub.modules.auth.controller;

import com.wavehub.common.api.ApiResponse;
import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {
    @GetMapping("/api/auth/session")
    public ApiResponse<?> session(Principal principal) {
        return ApiResponse.success(Map.of("username", principal.getName()));
    }
}
