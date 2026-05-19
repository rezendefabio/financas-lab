package com.laboratorio.financas.auditlog.infrastructure;

import com.laboratorio.financas.auditlog.domain.AuditEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publica {@link AuditEvent} no barramento de eventos de aplicacao.
 *
 * <p>Injetado pelos controllers de negocio, que chamam {@link #publish}
 * apos cada mutacao bem-sucedida. O processamento e assincrono via
 * {@code AuditEventListener}.
 */
@Component
public class AuditPublisher {

    private final ApplicationEventPublisher publisher;

    public AuditPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(AuditEvent event) {
        publisher.publishEvent(event);
    }
}
