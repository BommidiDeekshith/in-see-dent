package com.inseedent.service;

import com.inseedent.config.JwtTokenProvider;
import com.inseedent.domain.User;
import com.inseedent.dto.AuthRequest;
import com.inseedent.dto.AuthResponse;
import com.inseedent.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Register a new user
     */
    @Transactional
    public AuthResponse register(AuthRequest authRequest) {
        log.info("Registering new user with username: {}", authRequest.getUsername());

        // Validate input
        if (authRequest.getUsername() == null || authRequest.getUsername().trim().isEmpty()) {
            log.warn("Registration failed: username is empty");
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (authRequest.getPassword() == null || authRequest.getPassword().trim().isEmpty()) {
            log.warn("Registration failed: password is empty");
            throw new IllegalArgumentException("Password cannot be empty");
        }

        if (authRequest.getEmail() == null || authRequest.getEmail().trim().isEmpty()) {
            log.warn("Registration failed: email is empty");
            throw new IllegalArgumentException("Email cannot be empty");
        }

        // Check if username already exists
        if (userRepository.existsByUsername(authRequest.getUsername())) {
            log.warn("Registration failed: username already exists: {}", authRequest.getUsername());
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(authRequest.getEmail())) {
            log.warn("Registration failed: email already exists: {}", authRequest.getEmail());
            throw new IllegalArgumentException("Email already exists");
        }

        // Create new user
        User user = User.builder()
                .username(authRequest.getUsername())
                .email(authRequest.getEmail())
                .passwordHash(passwordEncoder.encode(authRequest.getPassword()))
                .fullName(authRequest.getFullName())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with id: {}", savedUser.getId());

        // Generate tokens
        String token = tokenProvider.generateToken(savedUser.getUsername());
        String refreshToken = tokenProvider.generateRefreshToken(savedUser.getUsername());

        return buildAuthResponse(savedUser, token, refreshToken);
    }

    /**
     * Login user and generate JWT token
     */
    public AuthResponse login(AuthRequest authRequest) {
        log.info("User login attempt with username: {}", authRequest.getUsername());

        if (authRequest.getUsername() == null || authRequest.getUsername().trim().isEmpty()) {
            log.warn("Login failed: username is empty");
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (authRequest.getPassword() == null || authRequest.getPassword().trim().isEmpty()) {
            log.warn("Login failed: password is empty");
            throw new IllegalArgumentException("Password cannot be empty");
        }

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()
                    )
            );

            // Get user details
            User user = userRepository.findByUsername(authRequest.getUsername())
                    .orElseThrow(() -> {
                        log.error("User not found after successful authentication: {}", authRequest.getUsername());
                        return new IllegalArgumentException("User not found");
                    });

            log.info("User logged in successfully: {}", authRequest.getUsername());

            // Generate tokens
            String token = tokenProvider.generateToken(user.getUsername());
            String refreshToken = tokenProvider.generateRefreshToken(user.getUsername());

            return buildAuthResponse(user, token, refreshToken);
        } catch (BadCredentialsException e) {
            log.warn("Login failed: invalid credentials for user: {}", authRequest.getUsername());
            throw new BadCredentialsException("Invalid username or password");
        } catch (Exception e) {
            log.error("Login failed for user: {}", authRequest.getUsername(), e);
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }

    /**
     * Refresh JWT token using refresh token
     */
    public AuthResponse refreshToken(String refreshToken) {
        log.debug("Attempting to refresh token");

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            log.warn("Refresh token is empty");
            throw new IllegalArgumentException("Refresh token cannot be empty");
        }

        if (!tokenProvider.validateToken(refreshToken)) {
            log.warn("Refresh token validation failed");
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        log.debug("Refreshing token for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found during token refresh: {}", username);
                    return new IllegalArgumentException("User not found");
                });

        // Generate new token and refresh token
        String newToken = tokenProvider.generateToken(user.getUsername());
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getUsername());

        log.info("Token refreshed successfully for user: {}", username);
        return buildAuthResponse(user, newToken, newRefreshToken);
    }

    /**
     * Build AuthResponse from user and tokens
     */
    private AuthResponse buildAuthResponse(User user, String token, String refreshToken) {
        long expiresIn = tokenProvider.getTokenExpirationTime(token) - System.currentTimeMillis();

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .token(token)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .build();
    }
}
