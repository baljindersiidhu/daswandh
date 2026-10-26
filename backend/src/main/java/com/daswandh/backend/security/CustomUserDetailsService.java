package com.daswandh.backend.security;

import com.daswandh.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Spring calls "username" what we call email.
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.daswandh.backend.entity.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Fully qualified name because Spring also has a class called User.
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name()) // becomes ROLE_CREATOR, which hasRole("CREATOR") matches
                .disabled(!user.isEnabled())
                .build();
    }
}