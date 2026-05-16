package com.inseedent.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Metric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @Column(nullable = false)
    private String metricName;

    private String serviceName;

    private String metricType; // gauge, counter, histogram, summary

    private Double value;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "jsonb")
    private String labels; // JSON

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
