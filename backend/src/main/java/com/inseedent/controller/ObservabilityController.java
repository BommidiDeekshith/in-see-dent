package com.inseedent.controller;

import com.inseedent.service.observability.MockObservabilityService;
import com.inseedent.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/observability")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Observability", description = "Telemetry ingestion from Prometheus, Loki, OTel, Grafana")
public class ObservabilityController {

    private final MockObservabilityService mockObservabilityService;

    @PostMapping("/incidents/{incidentId}/ingest-mock")
    @Operation(summary = "Ingest mock observability data for demo")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> ingestMock(@PathVariable Long incidentId) {
        return ResponseEntity.ok(ApiResponse.success(mockObservabilityService.ingestMockTelemetry(incidentId)));
    }
}
