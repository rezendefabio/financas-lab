package com.laboratorio.financas.importacao.application;

import com.laboratorio.financas.shared.domain.Money;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ImportarTransacoesCsvUseCase {

    private static final String CABECALHO_ESPERADO =
            "tipo,valor,moeda,data,descricao,contaId,contaDestinoId,categoriaId";

    private final TransacaoRepository transacaoRepository;

    public ImportarTransacoesCsvUseCase(TransacaoRepository transacaoRepository) {
        this.transacaoRepository = transacaoRepository;
    }

    public record Resultado(
            int totalLinhas,
            int importadas,
            int falhas,
            List<ErroImportacao> erros
    ) {
        public Resultado {
            erros = List.copyOf(erros);
        }
    }

    public record ErroImportacao(int linha, String motivo) { }

    private record LinhaValida(
            int numero,
            TipoTransacao tipo,
            BigDecimal valor,
            Currency moeda,
            LocalDate data,
            String descricao,
            UUID contaId,
            UUID contaDestinoId,
            UUID categoriaId
    ) { }

    private record LinhaInvalida(int numero, String motivo) { }

    public Resultado importar(byte[] conteudoCsv) {
        List<LinhaValida> linhasValidas = new ArrayList<>();
        List<LinhaInvalida> linhasInvalidas = new ArrayList<>();

        parsear(conteudoCsv, linhasValidas, linhasInvalidas);

        List<ErroImportacao> erros = new ArrayList<>();
        for (LinhaInvalida invalida : linhasInvalidas) {
            erros.add(new ErroImportacao(invalida.numero(), invalida.motivo()));
        }

        int persistidas = persistir(linhasValidas, erros);

        int total = linhasValidas.size() + linhasInvalidas.size();
        return new Resultado(total, persistidas, erros.size(), erros);
    }

    private void parsear(byte[] conteudoCsv, List<LinhaValida> validas, List<LinhaInvalida> invalidas) {
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
                parsearLinha(linha, numeroLinha, validas, invalidas);
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro ao ler CSV: " + e.getMessage(), e);
        }
    }

    private void parsearLinha(String linha, int numero, List<LinhaValida> validas,
            List<LinhaInvalida> invalidas) {
        String[] campos = linha.split(",", -1);
        if (campos.length != 8) {
            invalidas.add(new LinhaInvalida(numero,
                    "Numero de colunas invalido: esperado 8, encontrado " + campos.length));
            return;
        }

        String tipoStr = campos[0].trim();
        String valorStr = campos[1].trim();
        String moedaStr = campos[2].trim();
        String dataStr = campos[3].trim();
        String descricao = campos[4].trim();
        String contaIdStr = campos[5].trim();
        String contaDestinoIdStr = campos[6].trim();
        String categoriaIdStr = campos[7].trim();

        TipoTransacao tipo;
        try {
            tipo = TipoTransacao.valueOf(tipoStr);
        } catch (IllegalArgumentException e) {
            invalidas.add(new LinhaInvalida(numero, "Tipo invalido: " + tipoStr));
            return;
        }

        BigDecimal valor;
        try {
            valor = new BigDecimal(valorStr);
        } catch (NumberFormatException e) {
            invalidas.add(new LinhaInvalida(numero, "Valor invalido: " + valorStr));
            return;
        }

        Currency moeda;
        try {
            moeda = Currency.getInstance(moedaStr);
        } catch (IllegalArgumentException e) {
            invalidas.add(new LinhaInvalida(numero, "Moeda invalida: " + moedaStr));
            return;
        }

        LocalDate data;
        try {
            data = LocalDate.parse(dataStr);
        } catch (DateTimeParseException e) {
            invalidas.add(new LinhaInvalida(numero, "Data invalida: " + dataStr));
            return;
        }

        UUID contaId;
        try {
            contaId = UUID.fromString(contaIdStr);
        } catch (IllegalArgumentException e) {
            invalidas.add(new LinhaInvalida(numero, "contaId invalido: " + contaIdStr));
            return;
        }

        UUID contaDestinoId = null;
        if (!contaDestinoIdStr.isEmpty()) {
            try {
                contaDestinoId = UUID.fromString(contaDestinoIdStr);
            } catch (IllegalArgumentException e) {
                invalidas.add(new LinhaInvalida(numero, "contaDestinoId invalido: " + contaDestinoIdStr));
                return;
            }
        }

        UUID categoriaId = null;
        if (!categoriaIdStr.isEmpty()) {
            try {
                categoriaId = UUID.fromString(categoriaIdStr);
            } catch (IllegalArgumentException e) {
                invalidas.add(new LinhaInvalida(numero, "categoriaId invalido: " + categoriaIdStr));
                return;
            }
        }

        if (tipo == TipoTransacao.TRANSFERENCIA && contaDestinoId == null) {
            invalidas.add(new LinhaInvalida(numero, "TRANSFERENCIA exige contaDestinoId"));
            return;
        }

        validas.add(new LinhaValida(numero, tipo, valor, moeda, data, descricao,
                contaId, contaDestinoId, categoriaId));
    }

    @Transactional
    int persistir(List<LinhaValida> linhas, List<ErroImportacao> erros) {
        int importadas = 0;
        for (LinhaValida lv : linhas) {
            try {
                Transacao transacao = new Transacao(
                        lv.tipo(),
                        new Money(lv.valor(), lv.moeda()),
                        lv.data(),
                        lv.descricao(),
                        lv.contaId(),
                        lv.contaDestinoId(),
                        lv.categoriaId()
                );
                transacaoRepository.salvar(transacao);
                importadas++;
            } catch (Exception e) {
                erros.add(new ErroImportacao(lv.numero(), e.getMessage()));
            }
        }
        return importadas;
    }
}
