package com.daswandh.backend.controller;

import com.daswandh.backend.dto.AuthResponse;
import com.daswandh.backend.dto.LoginRequest;
import com.daswandh.backend.dto.RegisterRequest;
import com.daswandh.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // @Valid triggers the annotations on the DTO (@NotBlank, @Email...). Invalid input -> 400.
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);

    }
}