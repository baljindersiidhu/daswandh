package com.daswandh.backend.dto;

import com.daswandh.backend.entity.User;
import com.daswandh.backend.enums.Role;

// Never return the entity itself: it contains the password hash.
public record UserResponse(Long id, String fullName, String email, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}