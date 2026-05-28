package com.laboratorio.financas.carteira.application;

import com.laboratorio.financas.carteira.domain.Carteira;
import com.laboratorio.financas.carteira.domain.CarteiraNaoEncontradaException;
import com.laboratorio.financas.carteira.domain.CarteiraRepository;
import com.laboratorio.financas.carteira.domain.TipoCarteira;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AtualizarCarteiraUseCase {

    private final CarteiraRepository carteiraRepository;

    public AtualizarCarteiraUseCase(CarteiraRepository carteiraRepository) {
        this.carteiraRepository = carteiraRepository;
    }

    public record Comando(UUID id, String nome, TipoCarteira tipo) { }

    @Transactional
    public Carteira executar(Comando comando) {
        Carteira existente = carteiraRepository.buscarPorId(comando.id())
                .orElseThrow(() -> new CarteiraNaoEncontradaException(comando.id()));
        existente.atualizar(comando.nome(), comando.tipo());
        return carteiraRepository.atualizar(existente);
    }
}
