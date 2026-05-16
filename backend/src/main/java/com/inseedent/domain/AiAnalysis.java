package com.inseedent.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_analyses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @Column(nullable = false)
    private String analysisStatus; // pending, running, completed, failed

    private String rootCause;

    private Double confidenceScore;

    @Column(columnDefinition = "text[]")
    private String[] affectedServices;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(columnDefinition = "jsonb")
    private String findings; // JSON

    @Column(columnDefinition = "text")
    private String remediationSuggestions;

    @Column(columnDefinition = "jsonb")
    private String analysisData; // JSON

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<AnalysisArtifact> artifacts = new java.util.ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
