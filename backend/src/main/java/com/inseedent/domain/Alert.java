package com.inseedent.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @Column(nullable = false)
    private String alertSource; // Prometheus, Grafana, etc.

    @Column(nullable = false)
    private String alertName;

    private String severity;

    private String description;

    private String ruleName;

    @Column(columnDefinition = "TEXT")
    private String alertData;

    @Column(nullable = false)
    private LocalDateTime triggeredAt;

    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
