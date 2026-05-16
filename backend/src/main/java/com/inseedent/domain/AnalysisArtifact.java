package com.inseedent.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_artifacts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisArtifact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "analysis_id")
    private AiAnalysis analysis;

    @Column(nullable = false)
    private String artifactType; // logs_analysis, metrics_analysis, traces_analysis, deployment_analysis, correlation

    private String agentName;

    @Column(columnDefinition = "text")
    private String findings;

    private Double confidence;

    @Column(columnDefinition = "jsonb")
    private String rawData; // JSON

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
