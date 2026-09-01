package com.gridops.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryIngestRequest {

    @NotNull(message = "Asset ID is required")
    private UUID assetId;

    @NotNull(message = "Timestamp is required")
    private OffsetDateTime timestamp;

    @NotNull(message = "Actual output is required")
    private BigDecimal actualOutputKw;

    private BigDecimal expectedOutputKw;

    @NotNull(message = "Irradiance is required")
    private BigDecimal irradianceWM2;

    @NotNull(message = "Ambient temperature is required")
    private BigDecimal ambientTempC;

    private BigDecimal moduleTempC;

    @Builder.Default
    private Boolean isSimulated = true;
}
