package com.daswandh.backend.contoller;

import com.daswandh.backend.dto.HealthResponse;
import com.daswandh.backend.service.HealthService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    // Controllers stay thin: no logic here, just delegate to the service.
    @GetMapping
    public HealthResponse health() {
        return healthService.check();
    }
}
