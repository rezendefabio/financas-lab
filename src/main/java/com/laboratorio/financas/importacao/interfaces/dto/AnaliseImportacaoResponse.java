package com.laboratorio.financas.importacao.interfaces.dto;

import com.laboratorio.financas.importacao.application.AnalisarCsvUseCase;
import java.util.List;

public record AnaliseImportacaoResponse(
        int totalLinhas,
        int linhasValidas,
        int possivelDuplicatas,
        int errosParsing,
        List<AnaliseItemResponse> itens,
        List<ErroParsing> erros
) {

    public static AnaliseImportacaoResponse fromResultado(AnalisarCsvUseCase.Resultado r) {
        List<AnaliseItemResponse> itens = r.itens().stream()
                .map(i -> new AnaliseItemResponse(
                        i.linha(),
                        i.linhaCsvOriginal(),
                        i.tipo(),
                        i.valor(),
                        i.moeda(),
                        i.data().toString(),
                        i.descricao(),
                        i.contaId().toString(),
                        i.possivelDuplicata(),
                        i.transacaoExistenteId() != null ? i.transacaoExistenteId().toString() : null
                ))
                .toList();
        List<ErroParsing> erros = r.erros().stream()
                .map(e -> new ErroParsing(e.linha(), e.motivo()))
                .toList();
        return new AnaliseImportacaoResponse(
                r.totalLinhas(),
                r.linhasValidas(),
                r.possivelDuplicatas(),
                r.errosParsing(),
                itens,
                erros
        );
    }
}
