package com.inseedent.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_timelines")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentTimeline {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @Column(nullable = false)
    private String eventType; // alert, deployment, metric_spike, trace_error, log_error

    private String eventSource;

    @Column(columnDefinition = "text")
    private String description;

    private String severity;

    @Column(nullable = false)
    private LocalDateTime eventTime;

    @Column(columnDefinition = "jsonb")
    private String eventData; // JSON

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
