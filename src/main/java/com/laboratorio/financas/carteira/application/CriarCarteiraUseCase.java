package com.laboratorio.financas.carteira.application;

import com.laboratorio.financas.carteira.domain.Carteira;
import com.laboratorio.financas.carteira.domain.CarteiraRepository;
import com.laboratorio.financas.carteira.domain.TipoCarteira;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarCarteiraUseCase {

    private final CarteiraRepository carteiraRepository;

    public CriarCarteiraUseCase(CarteiraRepository carteiraRepository) {
        this.carteiraRepository = carteiraRepository;
    }

    public record Comando(UUID userId, UUID contaId, String nome, TipoCarteira tipo) { }

    @Transactional
    public Carteira executar(Comando comando) {
        Carteira carteira = new Carteira(
                comando.userId(),
                comando.contaId(),
                comando.nome(),
                comando.tipo()
        );
        return carteiraRepository.salvar(carteira);
    }
}
