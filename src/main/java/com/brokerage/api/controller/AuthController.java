package com.brokerage.api.controller;

import com.brokerage.api.dto.request.LoginRequest;
import com.brokerage.api.dto.response.JwtResponse;
import com.brokerage.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            log.info("Login attempt for user: {}", loginRequest.getUsername());
            JwtResponse response = authService.login(loginRequest);
            log.info("Login successful for user: {}", loginRequest.getUsername());
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            log.error("Login failed for user: {} - Bad credentials", loginRequest.getUsername());
            return ResponseEntity.status(401).body(
                    new ErrorResponse(401, "Invalid username or password", null)
            );
        } catch (Exception e) {
            log.error("Login failed for user: {} - Exception: {}", loginRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(500).body(
                    new ErrorResponse(500, "Authentication failed: " + e.getMessage(), null)
            );
        }
    }

    // Simple error class
    private static class ErrorResponse {
        private int status;
        private String message;
        private String path;
        private String timestamp;

        public ErrorResponse(int status, String message, String path) {
            this.status = status;
            this.message = message;
            this.path = path;
            this.timestamp = java.time.LocalDateTime.now().toString();
        }

        // Getters
        public int getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public String getPath() {
            return path;
        }

        public String getTimestamp() {
            return timestamp;
        }
    }
}