package com.gridops.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class AnalyticsDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TelemetryPointDto {
        private OffsetDateTime timestamp;
        @JsonProperty("actual_output_kw")
        private BigDecimal actualOutputKw;
        @JsonProperty("expected_output_kw")
        private BigDecimal expectedOutputKw;
        @JsonProperty("irradiance_w_m2")
        private BigDecimal irradianceWM2;
        @JsonProperty("ambient_temp_c")
        private BigDecimal ambientTempC;
        @JsonProperty("module_temp_c")
        private BigDecimal moduleTempC;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnomalyDetectionRequestDto {
        @JsonProperty("asset_id")
        private String assetId;
        @JsonProperty("asset_name")
        private String assetName;
        @JsonProperty("asset_type")
        private String assetType;
        @JsonProperty("rated_capacity_kw")
        private BigDecimal ratedCapacityKw;
        @JsonProperty("criticality_weight")
        private BigDecimal criticalityWeight;
        @JsonProperty("consecutive_threshold")
        private Integer consecutiveThreshold;
        @JsonProperty("deviation_percentage_threshold")
        private BigDecimal deviationPercentageThreshold;
        private List<TelemetryPointDto> readings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnomalyResponseDto {
        @JsonProperty("is_anomaly")
        private Boolean isAnomaly;
        private String severity;
        @JsonProperty("deviation_pct")
        private BigDecimal deviationPct;
        @JsonProperty("estimated_loss_kw")
        private BigDecimal estimatedLossKw;
        @JsonProperty("sustained_points_count")
        private Integer sustainedPointsCount;
        @JsonProperty("duration_minutes")
        private Integer durationMinutes;
        private String explanation;
        @JsonProperty("root_cause_candidate")
        private String rootCauseCandidate;
        @JsonProperty("confidence_score")
        private BigDecimal confidenceScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BaselineRequestDto {
        @JsonProperty("rated_capacity_kw")
        private BigDecimal ratedCapacityKw;
        @JsonProperty("irradiance_w_m2")
        private BigDecimal irradianceWM2;
        @JsonProperty("ambient_temp_c")
        private BigDecimal ambientTempC;
        @JsonProperty("module_temp_c")
        private BigDecimal moduleTempC;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BaselineResponseDto {
        @JsonProperty("expected_output_kw")
        private BigDecimal expectedOutputKw;
        @JsonProperty("efficiency_pct")
        private BigDecimal efficiencyPct;
        @JsonProperty("is_derated_by_heat")
        private Boolean isDeratedByHeat;
    }
}
