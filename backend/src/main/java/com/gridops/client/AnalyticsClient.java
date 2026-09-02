package com.gridops.client;

import com.gridops.dto.AnalyticsDtos.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

@Component
@Slf4j
public class AnalyticsClient {

    private final WebClient webClient;

    public AnalyticsClient(@Value("${analytics.service.url:http://localhost:8000}") String analyticsBaseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(analyticsBaseUrl)
                .build();
    }

    public BaselineResponseDto calculateBaseline(BaselineRequestDto request) {
        try {
            return webClient.post()
                    .uri("/api/v1/baseline")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(BaselineResponseDto.class)
                    .block();
        } catch (Exception e) {
            log.warn("Analytics service unavailable for baseline calculation, falling back to local heuristic: {}", e.getMessage());
            // Safe fallback heuristic
            BigDecimal irradiance = request.getIrradianceWM2() != null ? request.getIrradianceWM2() : BigDecimal.ZERO;
            BigDecimal capacity = request.getRatedCapacityKw() != null ? request.getRatedCapacityKw() : BigDecimal.ZERO;
            BigDecimal ratio = irradiance.divide(BigDecimal.valueOf(1000), 4, java.math.RoundingMode.HALF_UP);
            BigDecimal fallbackOutput = capacity.multiply(ratio).multiply(BigDecimal.valueOf(0.85));
            return BaselineResponseDto.builder()
                    .expectedOutputKw(fallbackOutput.max(BigDecimal.ZERO))
                    .efficiencyPct(BigDecimal.valueOf(85.0))
                    .isDeratedByHeat(false)
                    .build();
        }
    }

    public AnomalyResponseDto evaluateWindow(AnomalyDetectionRequestDto request) {
        try {
            return webClient.post()
                    .uri("/api/v1/anomalies/detect")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AnomalyResponseDto.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to reach analytics service for anomaly detection: {}", e.getMessage());
            return AnomalyResponseDto.builder()
                    .isAnomaly(false)
                    .explanation("Analytics service temporarily offline; anomaly evaluation postponed.")
                    .build();
        }
    }
}
