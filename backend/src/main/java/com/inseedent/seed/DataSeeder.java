package com.inseedent.seed;

import com.inseedent.domain.Incident;
import com.inseedent.domain.Runbook;
import com.inseedent.domain.User;
import com.inseedent.repository.IncidentRepository;
import com.inseedent.repository.RunbookRepository;
import com.inseedent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;
    private final RunbookRepository runbookRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        User admin = ensureAdmin();
        int incidentsAdded = seedIncidents();
        int runbooksAdded = seedRunbooks(admin);
        log.info("Seed complete: {} new incidents, {} new runbooks (login: admin / admin123)", incidentsAdded, runbooksAdded);
    }

    private User ensureAdmin() {
        return userRepository.findByUsername("admin").orElseGet(() ->
                userRepository.save(User.builder()
                        .username("admin")
                        .email("admin@inseedent.io")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .fullName("Demo Admin")
                        .build()));
    }

    private int seedIncidents() {
        List<IncidentSeed> seeds = List.of(
                seed("Checkout service outage — elevated 5xx errors", "critical", "investigating",
                        new String[]{"checkout-service", "payment-gateway"},
                        "Customer checkout failure rate spiked to 12% after deploy v2.4.1. Grafana alert HighErrorRate fired.",
                        1, "Grafana"),
                seed("Payment gateway latency degradation", "high", "open",
                        new String[]{"payment-gateway", "checkout-service"},
                        "P99 latency on /charge exceeded 2s SLO for 15 minutes. Upstream Stripe webhook delays observed.",
                        3, "Prometheus"),
                seed("Auth service JWT validation failures", "medium", "resolved",
                        new String[]{"auth-service", "redis-cache"},
                        "Intermittent 401s during peak traffic; Redis cache eviction increased auth DB load.",
                        26, "Loki"),
                seed("Inventory service stale stock after cache invalidation bug", "high", "investigating",
                        new String[]{"inventory-service", "redis-cache", "checkout-service"},
                        "Oversell incidents reported; cache TTL misconfiguration in v3.2.0 release.",
                        5, "Grafana"),
                seed("API gateway rate limiting misconfiguration", "medium", "open",
                        new String[]{"api-gateway"},
                        "Legitimate mobile clients throttled at 429; rate limit rules updated incorrectly in config push.",
                        8, "Prometheus"),
                seed("Search service Elasticsearch cluster yellow", "high", "investigating",
                        new String[]{"search-service", "elasticsearch"},
                        "Shard relocation stuck; query latency 4x normal affecting product catalog pages.",
                        12, "Grafana"),
                seed("Notification service email delivery backlog", "low", "open",
                        new String[]{"notification-service", "sqs-queue"},
                        "SQS depth > 50k; SendGrid API rate limit hit during marketing campaign.",
                        18, "CloudWatch"),
                seed("Recommendation ML model serving OOM kills", "critical", "investigating",
                        new String[]{"recommendation-service", "model-server"},
                        "Pods restarting under load; memory limit 2Gi insufficient for new embedding model.",
                        2, "Kubernetes"),
                seed("CDN edge 502 burst — static assets unavailable", "critical", "open",
                        new String[]{"cdn-edge", "api-gateway"},
                        "Regional PoP errors correlated with origin timeout; EU users most affected.",
                        0, "Grafana"),
                seed("Order service duplicate charges reported", "critical", "investigating",
                        new String[]{"order-service", "payment-gateway", "postgres-primary"},
                        "Idempotency key regression in v1.8.2; finance team escalated P1.",
                        4, "PagerDuty"),
                seed("Data pipeline lag — analytics dashboards stale", "medium", "open",
                        new String[]{"kafka-connect", "analytics-worker"},
                        "Consumer lag 2h on orders-topic; Flink job checkpoint failures since 06:00 UTC.",
                        10, "Datadog"),
                seed("Mobile BFF timeout on profile aggregation", "high", "resolved",
                        new String[]{"mobile-bff", "user-service", "auth-service"},
                        "Timeouts on /profile; downstream user-service p95 degraded after schema migration.",
                        48, "OpenTelemetry")
        );

        int added = 0;
        for (IncidentSeed s : seeds) {
            if (!incidentRepository.existsByTitle(s.title())) {
                incidentRepository.save(s.toIncident());
                added++;
            }
        }
        return added;
    }

    private int seedRunbooks(User admin) {
        List<RunbookSeed> seeds = List.of(
                new RunbookSeed(
                        "Checkout Service — Connection Pool Exhaustion",
                        "checkout-service",
                        new String[]{"database", "checkout", "pool"},
                        """
                        ## Symptoms
                        - HikariPool connection timeout errors
                        - Elevated 5xx on /api/checkout

                        ## Remediation
                        1. Scale checkout-service replicas
                        2. Increase Hikari maximumPoolSize
                        3. Roll back if correlated with deployment
                        """),
                new RunbookSeed(
                        "Payment Gateway — Upstream Timeout",
                        "payment-gateway",
                        new String[]{"payment", "timeout", "circuit-breaker"},
                        """
                        ## Symptoms
                        - p99 latency > 2s on /charge
                        - Circuit breaker OPEN in checkout-service

                        ## Remediation
                        1. Enable circuit breaker half-open probes
                        2. Scale payment-gateway pods
                        3. Verify Stripe/webhook health
                        """),
                new RunbookSeed(
                        "Redis Cache — Eviction & Auth Degradation",
                        "auth-service",
                        new String[]{"redis", "auth", "cache"},
                        """
                        ## Symptoms
                        - Redis memory maxmemory-policy volatile-lru evictions
                        - Auth DB QPS spike

                        ## Remediation
                        1. Increase Redis memory limit
                        2. Tune TTL on session keys
                        3. Scale auth-service read replicas
                        """)
        );

        int added = 0;
        for (RunbookSeed s : seeds) {
            if (!runbookRepository.existsByTitle(s.title())) {
                runbookRepository.save(Runbook.builder()
                        .title(s.title())
                        .description("Runbook for " + s.serviceName())
                        .serviceName(s.serviceName())
                        .tags(s.tags())
                        .content(s.content())
                        .createdBy(admin)
                        .build());
                added++;
            }
        }
        return added;
    }

    private static IncidentSeed seed(String title, String severity, String status,
                                     String[] services, String description,
                                     int hoursAgo, String detectedBy) {
        return new IncidentSeed(title, severity, status, services, description,
                LocalDateTime.now().minusHours(hoursAgo), detectedBy);
    }

    private record IncidentSeed(
            String title, String severity, String status, String[] services,
            String description, LocalDateTime startTime, String detectedBy) {
        Incident toIncident() {
            return Incident.builder()
                    .title(title)
                    .severity(severity)
                    .status(status)
                    .affectedServices(services)
                    .description(description)
                    .startTime(startTime)
                    .detectedBy(detectedBy)
                    .build();
        }
    }

    private record RunbookSeed(String title, String serviceName, String[] tags, String content) {}
}
