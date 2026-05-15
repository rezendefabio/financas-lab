package com.laboratorio.financas.instituicao.interfaces;

import com.laboratorio.financas.instituicao.domain.Instituicao;
import com.laboratorio.financas.instituicao.domain.TipoInstituicao;
import java.time.Instant;
import java.util.UUID;

public record InstituicaoResponse(
        UUID id,
        String nome,
        String codigoBanco,
        TipoInstituicao tipo,
        String logoUrl,
        boolean ativa,
        Instant criadoEm
) {
    public static InstituicaoResponse fromDomain(Instituicao instituicao) {
        return new InstituicaoResponse(
                instituicao.getId(),
                instituicao.getNome(),
                instituicao.getCodigoBanco(),
                instituicao.getTipo(),
                instituicao.getLogoUrl(),
                instituicao.isAtiva(),
                instituicao.getCriadoEm()
        );
    }
}
