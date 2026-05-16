package com.inseedent.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.inseedent.client.AiOrchestrationClient;
import com.inseedent.domain.Incident;
import com.inseedent.dto.SimilarIncidentDTO;
import com.inseedent.repository.IncidentRepository;
import com.inseedent.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Search", description = "Historical incident search and similarity")
public class SearchController {

    private final IncidentRepository incidentRepository;
    private final AiOrchestrationClient aiClient;

    @GetMapping("/incidents")
    @Operation(summary = "Search incidents by keyword")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String severity) {
        List<Incident> all = incidentRepository.findAll();
        List<Map<String, Object>> results = all.stream()
                .filter(i -> q == null || i.getTitle().toLowerCase().contains(q.toLowerCase())
                        || (i.getDescription() != null && i.getDescription().toLowerCase().contains(q.toLowerCase())))
                .filter(i -> severity == null || severity.equals(i.getSeverity()))
                .map(i -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", i.getId());
                    m.put("title", i.getTitle());
                    m.put("severity", i.getSeverity());
                    m.put("status", i.getStatus());
                    m.put("affectedServices", i.getAffectedServices());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/incidents/{incidentId}/similar")
    @Operation(summary = "Find similar historical incidents via RAG")
    public ResponseEntity<ApiResponse<List<SimilarIncidentDTO>>> similar(@PathVariable Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId).orElseThrow();
        Map<String, Object> payload = Map.of(
                "incident_id", incidentId,
                "title", incident.getTitle(),
                "description", incident.getDescription() != null ? incident.getDescription() : ""
        );
        JsonNode result = aiClient.searchSimilar(payload);
        List<SimilarIncidentDTO> similar = new ArrayList<>();
        if (result.has("results")) {
            result.get("results").forEach(n -> similar.add(SimilarIncidentDTO.builder()
                    .incidentId(n.path("incident_id").asLong())
                    .title(n.path("title").asText())
                    .similarityScore(n.path("score").asDouble())
                    .rootCause(n.path("root_cause").asText(null))
                    .build()));
        }
        return ResponseEntity.ok(ApiResponse.success(similar));
    }
}
