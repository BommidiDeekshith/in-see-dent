package com.inseedent.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "deployments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deployment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @Column(nullable = false)
    private String serviceName;

    private String version;

    private String deployedBy;

    @Column(nullable = false)
    private LocalDateTime deploymentTime;

    private String previousVersion;

    @Column(nullable = false)
    private Boolean rollbackAvailable;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (rollbackAvailable == null) {
            rollbackAvailable = false;
        }
    }
}
