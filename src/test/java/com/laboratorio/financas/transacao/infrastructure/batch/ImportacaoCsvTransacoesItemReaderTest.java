package com.laboratorio.financas.transacao.infrastructure.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.transacao.infrastructure.batch.ImportacaoCsvTransacoesItemReader.TransacaoCsvLinha;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.core.io.FileSystemResource;

class ImportacaoCsvTransacoesItemReaderTest {

    @Test
    void leArquivoCsvIgnorandoHeader(@TempDir Path tempDir) throws Exception {
        Path csv = tempDir.resolve("transacoes.csv");
        Files.writeString(
                csv,
                "tipo;valor;data;descricao;contaId;categoriaId\n"
                        + "RECEITA;100.00;2025-01-01;Salario;c1;cat1\n"
                        + "DESPESA;40.00;2025-01-02;Mercado;c1;cat2\n",
                StandardCharsets.UTF_8
        );

        FlatFileItemReader<TransacaoCsvLinha> reader =
                ImportacaoCsvTransacoesItemReader.build(new FileSystemResource(csv));
        reader.open(new ExecutionContext());

        try {
            TransacaoCsvLinha primeira = reader.read();
            TransacaoCsvLinha segunda = reader.read();
            TransacaoCsvLinha terceira = reader.read();

            assertThat(primeira).isNotNull();
            assertThat(primeira.tipo()).isEqualTo("RECEITA");
            assertThat(primeira.descricao()).isEqualTo("Salario");
            assertThat(segunda).isNotNull();
            assertThat(segunda.tipo()).isEqualTo("DESPESA");
            assertThat(terceira).isNull();
        } finally {
            reader.close();
        }
    }
}
