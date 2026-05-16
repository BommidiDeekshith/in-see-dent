package com.inseedent.controller;

import com.inseedent.dto.ServiceDependencyDTO;
import com.inseedent.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
@PreAuthorize("isAuthenticated()")
public class ServiceGraphController {

    @GetMapping("/dependencies")
    public ResponseEntity<ApiResponse<List<ServiceDependencyDTO>>> dependencies() {
        List<ServiceDependencyDTO> graph = List.of(
                ServiceDependencyDTO.builder().id("api-gateway").label("API Gateway").status("healthy")
                        .dependencies(List.of("auth-service", "checkout-service")).build(),
                ServiceDependencyDTO.builder().id("checkout-service").label("Checkout Service").status("degraded")
                        .dependencies(List.of("payment-gateway", "inventory-service")).build(),
                ServiceDependencyDTO.builder().id("payment-gateway").label("Payment Gateway").status("critical")
                        .dependencies(List.of("postgres-primary")).build(),
                ServiceDependencyDTO.builder().id("inventory-service").label("Inventory Service").status("healthy")
                        .dependencies(List.of("redis-cache")).build(),
                ServiceDependencyDTO.builder().id("auth-service").label("Auth Service").status("healthy")
                        .dependencies(List.of("postgres-primary")).build()
        );
        return ResponseEntity.ok(ApiResponse.success(graph));
    }
}
