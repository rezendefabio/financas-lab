package com.laboratorio.financas.shared.infrastructure.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/healthcheck")
public class HealthcheckController {

    @GetMapping
    public HealthcheckResponse healthcheck() {
        return new HealthcheckResponse("ok", Instant.now());
    }
}
