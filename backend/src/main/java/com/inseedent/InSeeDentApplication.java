package com.inseedent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
public class InSeeDentApplication {
    public static void main(String[] args) {
        SpringApplication.run(InSeeDentApplication.class, args);
    }
}
