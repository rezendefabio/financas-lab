package com.laboratorio.financas.anexo.interfaces.dto;

import com.laboratorio.financas.anexo.domain.Anexo;
import java.time.Instant;
import java.util.UUID;

public record AnexoResponse(
        UUID id,
        String nome,
        String tipoConteudo,
        long tamanho,
        String entidadeTipo,
        UUID entidadeId,
        Instant criadoEm
) {

    public static AnexoResponse de(Anexo anexo) {
        return new AnexoResponse(
                anexo.getId(),
                anexo.getNome(),
                anexo.getTipoConteudo(),
                anexo.getTamanho(),
                anexo.getEntidadeTipo(),
                anexo.getEntidadeId(),
                anexo.getCriadoEm()
        );
    }
}
