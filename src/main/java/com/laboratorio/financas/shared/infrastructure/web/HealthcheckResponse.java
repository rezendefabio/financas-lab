package com.laboratorio.financas.shared.infrastructure.web;

import java.time.Instant;

public record HealthcheckResponse(String status, Instant timestamp) {
}
