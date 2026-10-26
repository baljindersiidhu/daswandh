package com.daswandh.backend.dto;

public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserResponse user) {}