package com.laboratorio.financas.anotacao.application;

import com.laboratorio.financas.anotacao.domain.Anotacao;
import com.laboratorio.financas.anotacao.domain.AnotacaoRepository;
import com.laboratorio.financas.anotacao.domain.PrioridadeAnotacao;
import com.laboratorio.financas.anotacao.domain.TipoAnotacao;
import com.laboratorio.financas.shared.domain.Money;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CriarAnotacaoUseCase {

    private final AnotacaoRepository repository;

    public CriarAnotacaoUseCase(AnotacaoRepository repository) {
        this.repository = repository;
    }

    public Anotacao executar(UUID usuarioId, String titulo, String conteudo, TipoAnotacao tipo,
                             PrioridadeAnotacao prioridade, Money valor, LocalDate dataReferencia) {
        Anotacao anotacao = new Anotacao(usuarioId, titulo, conteudo, tipo, prioridade, valor, dataReferencia);
        return repository.salvar(anotacao);
    }
}
