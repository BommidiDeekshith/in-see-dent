package com.inseedent.controller;

import com.inseedent.dto.AnalysisResultDTO;
import com.inseedent.service.analysis.AnalysisService;
import com.inseedent.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents/{incidentId}/analysis")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "AI Analysis", description = "Multi-agent root cause analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping
    @Operation(summary = "Trigger AI root cause analysis")
    public ResponseEntity<ApiResponse<AnalysisResultDTO>> trigger(@PathVariable Long incidentId) {
        return ResponseEntity.accepted()
                .body(ApiResponse.success(analysisService.triggerAnalysis(incidentId)));
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest analysis result")
    public ResponseEntity<ApiResponse<AnalysisResultDTO>> latest(@PathVariable Long incidentId) {
        return ResponseEntity.ok(ApiResponse.success(analysisService.getLatestAnalysis(incidentId)));
    }

    @GetMapping
    @Operation(summary = "List all analyses for incident")
    public ResponseEntity<ApiResponse<List<AnalysisResultDTO>>> list(@PathVariable Long incidentId) {
        return ResponseEntity.ok(ApiResponse.success(analysisService.listAnalyses(incidentId)));
    }
}
