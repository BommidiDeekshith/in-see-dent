package com.inseedent.seed;

import com.inseedent.domain.*;
import com.inseedent.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
        if (userRepository.count() > 0) {
            log.info("Database already seeded, skipping");
            return;
        }
        log.info("Seeding demo data for InSeeDent...");

        User admin = userRepository.save(User.builder()
                .username("admin")
                .email("admin@inseedent.io")
                .passwordHash(passwordEncoder.encode("admin123"))
                .fullName("Demo Admin")
                .build());

        Incident[] incidents = {
                buildIncident("Checkout service outage — elevated 5xx", "critical", "investigating",
                        new String[]{"checkout-service", "payment-gateway"},
                        "Spike in checkout failures after deployment v2.4.1"),
                buildIncident("Payment gateway latency degradation", "high", "open",
                        new String[]{"payment-gateway"},
                        "P99 latency exceeded SLO for 15 minutes"),
                buildIncident("Auth service token validation errors", "medium", "resolved",
                        new String[]{"auth-service"},
                        "Intermittent JWT validation failures during peak traffic")
        };

        for (Incident i : incidents) {
            incidentRepository.save(i);
        }

        runbookRepository.save(Runbook.builder()
                .title("Checkout Service — Connection Pool Exhaustion")
                .description("Runbook for DB pool saturation incidents")
                .serviceName("checkout-service")
                .tags(new String[]{"database", "checkout", "pool"})
                .content("""
                    ## Symptoms
                    - HikariPool connection timeout errors
                    - Elevated 5xx on /api/checkout
                    
                    ## Remediation
                    1. Scale checkout-service replicas
                    2. Increase Hikari maximumPoolSize
                    3. Roll back if correlated with deployment
                    4. Enable circuit breaker on payment-gateway calls
                    """)
                .createdBy(admin)
                .build());

        log.info("Seeded user admin/admin123 and {} incidents", incidents.length);
    }

    private Incident buildIncident(String title, String severity, String status,
                                   String[] services, String description) {
        return Incident.builder()
                .title(title)
                .description(description)
                .severity(severity)
                .status(status)
                .affectedServices(services)
                .startTime(LocalDateTime.now().minusHours(2))
                .detectedBy("Grafana")
                .build();
    }
}
