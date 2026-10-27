package com.daswandh.backend.service;

import com.daswandh.backend.dto.AuthResponse;
import com.daswandh.backend.dto.LoginRequest;
import com.daswandh.backend.dto.RegisterRequest;
import com.daswandh.backend.dto.UserResponse;
import com.daswandh.backend.entity.User;
import com.daswandh.backend.enums.Role;
import com.daswandh.backend.exception.ConflictException;
import com.daswandh.backend.exception.ForbiddenException;
import com.daswandh.backend.exception.UnauthorizedException;
import com.daswandh.backend.repository.UserRepository;
import com.daswandh.backend.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new ForbiddenException("Admin accounts cannot be self-registered");
        }
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        // We store only the BCrypt hash, never the raw password.
        User user = new User(email, passwordEncoder.encode(request.password()),
                request.fullName().trim(), request.role());
        userRepository.save(user);
        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalize(request.email());
        try {
            // Checks the password hash and whether the account is enabled.
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException e) {
            // Same message for "no such user" and "wrong password", so attackers can't discover which emails exist.
            throw new UnauthorizedException("Invalid email or password");
        }
        User user = userRepository.findByEmail(email).orElseThrow(); // exists: authentication just succeeded
        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), "Bearer",
                jwtService.getExpirationSeconds(), UserResponse.from(user));
    }

    // Emails are case-insensitive in practice, so we store them in one canonical form.
    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}