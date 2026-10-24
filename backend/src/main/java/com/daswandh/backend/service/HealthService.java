package com.daswandh.backend.service;

import com.daswandh.backend.dto.HealthResponse;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class HealthService {
    private final JdbcTemplate jdbcTemplate;

    // Constructor injection: Spring passes in the JdbcTemplate it already created
    public HealthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public HealthResponse check() {
        String dbStatus;
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class); // cheapest possible DB ping
            dbStatus = "UP";
        } catch (DataAccessException e) {
            dbStatus = "DOWN";
        }
        String overall = dbStatus.equals("UP") ? "UP" : "DEGRADED";
        return new HealthResponse(overall, "daswandh-backend", dbStatus, Instant.now());
    }
}
