package com.laboratorio.financas.importacao.application;

import com.laboratorio.financas.transacao.domain.FiltrosTransacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Analisa um CSV de importacao de transacoes sem persistir nada,
 * detectando provaveis duplicatas comparando cada linha valida
 * com transacoes ja existentes (mesma conta + data + valor + tipo).
 */
@Component
public class AnalisarCsvUseCase {

    private static final String CABECALHO_ESPERADO =
            "tipo,valor,moeda,data,descricao,contaId,contaDestinoId,categoriaId";

    private final TransacaoRepository transacaoRepository;

    public AnalisarCsvUseCase(TransacaoRepository transacaoRepository) {
        this.transacaoRepository = transacaoRepository;
    }

    public record ItemAnalise(
            int linha,
            String linhaCsvOriginal,
            String tipo,
            BigDecimal valor,
            String moeda,
            LocalDate data,
            String descricao,
            UUID contaId,
            boolean possivelDuplicata,
            UUID transacaoExistenteId
    ) { }

    public record ErroParsing(int linha, String motivo) { }

    public record Resultado(
            int totalLinhas,
            int linhasValidas,
            int possivelDuplicatas,
            int errosParsing,
            List<ItemAnalise> itens,
            List<ErroParsing> erros
    ) {
        public Resultado {
            itens = List.copyOf(itens);
            erros = List.copyOf(erros);
        }
    }

    private record LinhaValida(
            int numero,
            String linhaCsvOriginal,
            TipoTransacao tipo,
            BigDecimal valor,
            Currency moeda,
            LocalDate data,
            String descricao,
            UUID contaId
    ) { }

    public Resultado analisar(byte[] conteudoCsv) {
        List<LinhaValida> linhasValidas = new ArrayList<>();
        List<ErroParsing> erros = new ArrayList<>();

        parsear(conteudoCsv, linhasValidas, erros);

        List<ItemAnalise> itens = new ArrayList<>();
        int possivelDuplicatas = 0;
        for (LinhaValida lv : linhasValidas) {
            UUID existenteId = detectarDuplicata(lv);
            boolean possivel = existenteId != null;
            if (possivel) {
                possivelDuplicatas++;
            }
            itens.add(new ItemAnalise(
                    lv.numero(),
                    lv.linhaCsvOriginal(),
                    lv.tipo().name(),
                    lv.valor(),
                    lv.moeda().getCurrencyCode(),
                    lv.data(),
                    lv.descricao(),
                    lv.contaId(),
                    possivel,
                    existenteId
            ));
        }

        int total = linhasValidas.size() + erros.size();
        return new Resultado(total, linhasValidas.size(), possivelDuplicatas,
                erros.size(), itens, erros);
    }

    private UUID detectarDuplicata(LinhaValida lv) {
        FiltrosTransacao filtros = new FiltrosTransacao(
                lv.contaId(), lv.data(), lv.data(), lv.tipo(), null
        );
        Page<Transacao> page = transacaoRepository.listarComFiltros(filtros, PageRequest.of(0, 50));
        for (Transacao t : page.getContent()) {
            if (t.getValor().valor().compareTo(lv.valor()) == 0
                    && t.getValor().moeda().equals(lv.moeda())) {
                return t.getId();
            }
        }
        return null;
    }

    private void parsear(byte[] conteudoCsv, List<LinhaValida> validas, List<ErroParsing> erros) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(conteudoCsv), StandardCharsets.UTF_8))) {

            String cabecalho = reader.readLine();
            if (cabecalho == null || !cabecalho.trim().equals(CABECALHO_ESPERADO)) {
                throw new IllegalArgumentException(
                        "Cabecalho CSV invalido. Formato esperado: " + CABECALHO_ESPERADO);
            }

            String linha;
            int numeroLinha = 1;
            while ((linha = reader.readLine()) != null) {
                numeroLinha++;
                if (linha.trim().isBlank()) {
                    continue;
                }
                parsearLinha(linha, numeroLinha, validas, erros);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro ao ler CSV: " + e.getMessage(), e);
        }
    }

    private void parsearLinha(String linha, int numero, List<LinhaValida> validas,
            List<ErroParsing> erros) {
        String[] campos = linha.split(",", -1);
        if (campos.length != 8) {
            erros.add(new ErroParsing(numero,
                    "Numero de colunas invalido: esperado 8, encontrado " + campos.length));
            return;
        }

        String tipoStr = campos[0].trim();
        String valorStr = campos[1].trim();
        String moedaStr = campos[2].trim();
        String dataStr = campos[3].trim();
        String descricao = campos[4].trim();
        String contaIdStr = campos[5].trim();

        TipoTransacao tipo;
        try {
            tipo = TipoTransacao.valueOf(tipoStr);
        } catch (IllegalArgumentException e) {
            erros.add(new ErroParsing(numero, "Tipo invalido: " + tipoStr));
            return;
        }

        BigDecimal valor;
        try {
            valor = new BigDecimal(valorStr);
        } catch (NumberFormatException e) {
            erros.add(new ErroParsing(numero, "Valor invalido: " + valorStr));
            return;
        }

        Currency moeda;
        try {
            moeda = Currency.getInstance(moedaStr);
        } catch (IllegalArgumentException e) {
            erros.add(new ErroParsing(numero, "Moeda invalida: " + moedaStr));
            return;
        }

        LocalDate data;
        try {
            data = LocalDate.parse(dataStr);
        } catch (DateTimeParseException e) {
            erros.add(new ErroParsing(numero, "Data invalida: " + dataStr));
            return;
        }

        UUID contaId;
        try {
            contaId = UUID.fromString(contaIdStr);
        } catch (IllegalArgumentException e) {
            erros.add(new ErroParsing(numero, "contaId invalido: " + contaIdStr));
            return;
        }

        validas.add(new LinhaValida(numero, linha, tipo, valor, moeda, data, descricao, contaId));
    }
}
