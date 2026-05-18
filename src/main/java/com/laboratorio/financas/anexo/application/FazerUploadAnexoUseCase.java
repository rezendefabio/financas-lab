package com.laboratorio.financas.anexo.application;

import com.laboratorio.financas.anexo.domain.Anexo;
import com.laboratorio.financas.anexo.domain.AnexoRepository;
import com.laboratorio.financas.anexo.domain.ArmazenamentoService;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FazerUploadAnexoUseCase {

    private static final long TAMANHO_MAXIMO_BYTES = 10L * 1024 * 1024;

    private final AnexoRepository anexoRepository;
    private final ArmazenamentoService armazenamentoService;

    public FazerUploadAnexoUseCase(AnexoRepository anexoRepository,
                                   ArmazenamentoService armazenamentoService) {
        this.anexoRepository = anexoRepository;
        this.armazenamentoService = armazenamentoService;
    }

    public record Comando(
            String nome,
            String tipoConteudo,
            long tamanho,
            String entidadeTipo,
            UUID entidadeId,
            InputStream conteudo
    ) { }

    @Transactional
    public Anexo executar(Comando comando) {
        if (comando.tamanho() > TAMANHO_MAXIMO_BYTES) {
            throw new IllegalArgumentException(
                    "Arquivo excede o tamanho maximo permitido de 10MB");
        }
        Anexo anexo = new Anexo(
                comando.nome(),
                comando.tipoConteudo(),
                comando.tamanho(),
                comando.entidadeTipo(),
                comando.entidadeId()
        );
        armazenamentoService.upload(
                anexo.getChaveArmazenamento(),
                comando.conteudo(),
                comando.tamanho(),
                comando.tipoConteudo()
        );
        return anexoRepository.salvar(anexo);
    }
}
