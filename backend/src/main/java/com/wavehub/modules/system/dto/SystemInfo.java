package com.wavehub.modules.system.dto;

import java.time.Instant;

public record SystemInfo(String name, String version, String databaseMode, Instant serverTime) {}
