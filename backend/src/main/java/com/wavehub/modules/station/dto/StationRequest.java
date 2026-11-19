package com.wavehub.modules.station.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record StationRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String code,
        @NotBlank @Size(max = 128) String name,
        @NotNull @DecimalMin("-180") @DecimalMax("180") @Digits(integer = 4, fraction = 7) BigDecimal longitude,
        @NotNull @DecimalMin("-90") @DecimalMax("90") @Digits(integer = 3, fraction = 7) BigDecimal latitude,
        @Digits(integer = 9, fraction = 3) BigDecimal elevationM,
        @NotNull Boolean enabled,
        @Size(max = 2000) String description) {}
