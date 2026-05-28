package com.laboratorio.financas.carteira.application;

import com.laboratorio.financas.carteira.domain.CarteiraNaoEncontradaException;
import com.laboratorio.financas.carteira.domain.CarteiraRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeletarCarteiraUseCase {

    private final CarteiraRepository carteiraRepository;

    public DeletarCarteiraUseCase(CarteiraRepository carteiraRepository) {
        this.carteiraRepository = carteiraRepository;
    }

    @Transactional
    public void executar(UUID id) {
        carteiraRepository.buscarPorId(id)
                .orElseThrow(() -> new CarteiraNaoEncontradaException(id));
        carteiraRepository.deletar(id);
    }
}
