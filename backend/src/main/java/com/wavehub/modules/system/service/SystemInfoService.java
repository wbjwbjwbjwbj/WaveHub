package com.wavehub.modules.system.service;

import com.wavehub.modules.system.dto.SystemInfo;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

@Service
public class SystemInfoService {
    private final Environment environment;
    private final String version;

    public SystemInfoService(Environment environment, @Value("${wavehub.version}") String version) {
        this.environment = environment;
        this.version = version;
    }

    public SystemInfo getInfo() {
        String mode = environment.acceptsProfiles(Profiles.of("kingbase")) ? "kingbase" : "standalone";
        return new SystemInfo("WaveHub", version, mode, Instant.now());
    }
}
