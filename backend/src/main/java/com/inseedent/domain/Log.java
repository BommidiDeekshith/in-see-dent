package com.inseedent.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "incident_id")
    private Incident incident;

    private String serviceName;

    private String logLevel;

    @Column(columnDefinition = "text")
    private String message;

    private String logSource; // Loki, etc.

    private String traceId;

    private String spanId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "jsonb")
    private String logData; // JSON

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
