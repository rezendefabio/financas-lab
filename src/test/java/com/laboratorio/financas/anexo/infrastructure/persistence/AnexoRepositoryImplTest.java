package com.laboratorio.financas.anexo.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.anexo.domain.Anexo;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AnexoRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private AnexoRepositoryImpl repository;

    @Autowired
    private AnexoJpaRepository jpaRepository;

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    private Anexo novoAnexo(String entidadeTipo, UUID entidadeId) {
        return new Anexo("comprovante.pdf", "application/pdf", 4096, entidadeTipo, entidadeId);
    }

    @Test
    void salvarPersisteAnexoERetornaInstanciaEquivalente() {
        // Given
        UUID entidadeId = UUID.randomUUID();
        Anexo novo = novoAnexo("TRANSACAO", entidadeId);

        // When
        Anexo salvo = repository.salvar(novo);

        // Then
        assertThat(salvo.getId()).isEqualTo(novo.getId());
        assertThat(salvo.getNome()).isEqualTo("comprovante.pdf");
        assertThat(salvo.getTipoConteudo()).isEqualTo("application/pdf");
        assertThat(salvo.getTamanho()).isEqualTo(4096L);
        assertThat(salvo.getChaveArmazenamento()).isEqualTo(novo.getChaveArmazenamento());
        assertThat(salvo.getEntidadeTipo()).isEqualTo("TRANSACAO");
        assertThat(salvo.getEntidadeId()).isEqualTo(entidadeId);
    }

    @Test
    void buscarPorIdRetornaAnexoQuandoExiste() {
        // Given
        Anexo novo = novoAnexo("CONTA", UUID.randomUUID());
        repository.salvar(novo);

        // When
        Optional<Anexo> resultado = repository.buscarPorId(novo.getId());

        // Then
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(novo.getId());
        assertThat(resultado.get().getChaveArmazenamento())
                .isEqualTo(novo.getChaveArmazenamento());
    }

    @Test
    void buscarPorIdRetornaVazioQuandoNaoExiste() {
        // When
        Optional<Anexo> resultado = repository.buscarPorId(UUID.randomUUID());

        // Then
        assertThat(resultado).isEmpty();
    }

    @Test
    void listarPorEntidadeRetornaApenasAnexosDaEntidadeInformada() {
        // Given
        UUID transacaoId = UUID.randomUUID();
        UUID outraTransacaoId = UUID.randomUUID();
        repository.salvar(novoAnexo("TRANSACAO", transacaoId));
        repository.salvar(novoAnexo("TRANSACAO", transacaoId));
        repository.salvar(novoAnexo("TRANSACAO", outraTransacaoId));

        // When
        List<Anexo> resultado = repository.listarPorEntidade("TRANSACAO", transacaoId);

        // Then
        assertThat(resultado).hasSize(2);
        assertThat(resultado).allMatch(a -> a.getEntidadeId().equals(transacaoId));
    }

    @Test
    void listarPorEntidadeIsolaPorEntidadeTipo() {
        // Given
        UUID mesmoId = UUID.randomUUID();
        repository.salvar(novoAnexo("TRANSACAO", mesmoId));
        repository.salvar(novoAnexo("CONTA", mesmoId));

        // When
        List<Anexo> transacao = repository.listarPorEntidade("TRANSACAO", mesmoId);
        List<Anexo> conta = repository.listarPorEntidade("CONTA", mesmoId);

        // Then
        assertThat(transacao).hasSize(1);
        assertThat(conta).hasSize(1);
    }

    @Test
    void listarPorEntidadeRetornaListaVaziaQuandoNenhumAnexo() {
        // When
        List<Anexo> resultado = repository.listarPorEntidade("META", UUID.randomUUID());

        // Then
        assertThat(resultado).isEmpty();
    }

    @Test
    void removerApagaAnexoPersistido() {
        // Given
        Anexo novo = novoAnexo("META", UUID.randomUUID());
        repository.salvar(novo);

        // When
        repository.remover(novo.getId());

        // Then
        assertThat(repository.buscarPorId(novo.getId())).isEmpty();
    }
}
