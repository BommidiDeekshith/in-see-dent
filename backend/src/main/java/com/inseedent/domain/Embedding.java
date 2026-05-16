package com.inseedent.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "embeddings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Embedding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entityType; // incident, runbook, postmortem

    @Column(nullable = false)
    private Long entityId;

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private String embeddingVector; // Stored as string, LangChain will handle actual vector operations

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
