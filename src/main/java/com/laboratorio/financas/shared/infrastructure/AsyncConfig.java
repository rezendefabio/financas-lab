package com.laboratorio.financas.shared.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Habilita execucao assincrona de metodos anotados com {@code @Async}.
 *
 * <p>Necessario para que {@code @EventListener} assincronos (ex.:
 * {@code OrcamentoProgressoListener}) rodem fora da thread que disparou
 * o evento -- garantindo que falha no listener nao afete a operacao origem.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
