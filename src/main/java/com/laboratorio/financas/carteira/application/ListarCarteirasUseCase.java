package com.laboratorio.financas.carteira.application;

import com.laboratorio.financas.carteira.domain.Carteira;
import com.laboratorio.financas.carteira.domain.CarteiraRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListarCarteirasUseCase {

    private final CarteiraRepository carteiraRepository;

    public ListarCarteirasUseCase(CarteiraRepository carteiraRepository) {
        this.carteiraRepository = carteiraRepository;
    }

    @Transactional(readOnly = true)
    public List<Carteira> executar() {
        return carteiraRepository.listarTodos();
    }
}
