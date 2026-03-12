package com.fairsplit.api.controller;

import com.fairsplit.api.dto.AuthResponse;
import com.fairsplit.api.dto.LoginRequest;
import com.fairsplit.api.dto.RegisterRequest;
import com.fairsplit.api.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest registerRequest) {
        String token = authService.register(registerRequest.email(),
                registerRequest.password(), registerRequest.displayName());
        return ResponseEntity.status(201).body(new AuthResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        String token = authService.login(loginRequest.email(), loginRequest.password());
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
