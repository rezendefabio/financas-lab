package com.laboratorio.financas.incidente.application;

import com.laboratorio.financas.incidente.domain.ErroRegistrado;
import com.laboratorio.financas.incidente.domain.ErroRegistradoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RegistrarErroUseCase {

    private final ErroRegistradoRepository erroRegistradoRepository;

    public RegistrarErroUseCase(ErroRegistradoRepository erroRegistradoRepository) {
        this.erroRegistradoRepository = erroRegistradoRepository;
    }

    public record Comando(String operacao, String classeErro, String mensagem, String stackTrace) { }

    @Transactional
    public String executar(Comando comando) {
        ErroRegistrado erro = new ErroRegistrado(
                comando.operacao(),
                comando.classeErro(),
                comando.mensagem(),
                comando.stackTrace()
        );
        return erroRegistradoRepository.salvar(erro).getCodigo();
    }
}
