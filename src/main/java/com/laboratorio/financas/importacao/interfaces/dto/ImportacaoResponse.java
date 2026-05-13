package com.laboratorio.financas.importacao.interfaces.dto;

import com.laboratorio.financas.importacao.application.ImportarTransacoesCsvUseCase;
import java.util.List;

public record ImportacaoResponse(
        int totalLinhas,
        int importadas,
        int falhas,
        List<ErroImportacaoResponse> erros
) {

    public record ErroImportacaoResponse(int linha, String motivo) { }

    public static ImportacaoResponse fromResultado(ImportarTransacoesCsvUseCase.Resultado r) {
        List<ErroImportacaoResponse> erros = r.erros().stream()
                .map(e -> new ErroImportacaoResponse(e.linha(), e.motivo()))
                .toList();
        return new ImportacaoResponse(r.totalLinhas(), r.importadas(), r.falhas(), erros);
    }
}
