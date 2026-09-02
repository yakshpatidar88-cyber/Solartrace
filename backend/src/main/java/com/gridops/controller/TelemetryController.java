package com.gridops.controller;

import com.gridops.domain.TelemetryReading;
import com.gridops.dto.TelemetryIngestRequest;
import com.gridops.repository.TelemetryReadingRepository;
import com.gridops.service.TelemetryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/telemetry")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TelemetryController {

    private final TelemetryService telemetryService;
    private final TelemetryReadingRepository telemetryRepository;

    @PostMapping("/ingest")
    public ResponseEntity<TelemetryReading> ingest(@Valid @RequestBody TelemetryIngestRequest request) {
        return ResponseEntity.ok(telemetryService.ingestReading(request));
    }

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<List<TelemetryReading>> getRecentTelemetry(
            @PathVariable UUID assetId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(telemetryRepository.findByAssetIdOrderByTimestampDesc(assetId, PageRequest.of(0, limit)));
    }

    @GetMapping("/asset/{assetId}/range")
    public ResponseEntity<List<TelemetryReading>> getTelemetryRange(
            @PathVariable UUID assetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end) {
        return ResponseEntity.ok(telemetryRepository.findByAssetIdAndTimestampBetween(assetId, start, end));
    }
}
