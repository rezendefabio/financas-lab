package com.laboratorio.financas.instituicao.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.instituicao.domain.Instituicao;
import com.laboratorio.financas.instituicao.domain.TipoInstituicao;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InstituicaoRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private InstituicaoRepositoryImpl repository;

    @Autowired
    private InstituicaoJpaRepository jpaRepository;

    @BeforeEach
    void limparAntes() {
        jpaRepository.deleteAll();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    @Test
    void savePersisteERetornaInstanciaEquivalente() {
        // Given
        Instituicao nova = new Instituicao("Nubank", "260", TipoInstituicao.BANCO_DIGITAL, null, true);

        // When
        Instituicao salva = repository.save(nova);

        // Then
        assertThat(salva.getId()).isEqualTo(nova.getId());
        assertThat(salva.getNome()).isEqualTo("Nubank");
        assertThat(salva.getTipo()).isEqualTo(TipoInstituicao.BANCO_DIGITAL);
        assertThat(salva.isAtiva()).isTrue();
    }

    @Test
    void findByIdRetornaInstituicaoQuandoExiste() {
        // Given
        Instituicao nova = new Instituicao("Inter", "077", TipoInstituicao.BANCO_DIGITAL, null, true);
        repository.save(nova);

        // When
        Optional<Instituicao> resultado = repository.findById(nova.getId());

        // Then
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(nova.getId());
        assertThat(resultado.get().getNome()).isEqualTo("Inter");
    }

    @Test
    void findByIdRetornaVazioQuandoNaoExiste() {
        // When
        Optional<Instituicao> resultado = repository.findById(UUID.randomUUID());

        // Then
        assertThat(resultado).isEmpty();
    }

    @Test
    void findAllRetornaTodasAsInstituicoes() {
        // Given
        repository.save(new Instituicao("Nubank", "260", TipoInstituicao.BANCO_DIGITAL, null, true));
        repository.save(new Instituicao("Inter", "077", TipoInstituicao.BANCO_DIGITAL, null, true));
        repository.save(new Instituicao("Banco Inativo", null, TipoInstituicao.OUTRO, null, false));

        // When
        List<Instituicao> todas = repository.findAll();

        // Then
        assertThat(todas).hasSize(3);
    }

    @Test
    void findAllAtivasRetornaApenasInstituicoesAtivas() {
        // Given -- 2 ativas + 1 inativa
        repository.save(new Instituicao("Nubank", "260", TipoInstituicao.BANCO_DIGITAL, null, true));
        repository.save(new Instituicao("Inter", "077", TipoInstituicao.BANCO_DIGITAL, null, true));
        repository.save(new Instituicao("Banco Inativo", null, TipoInstituicao.OUTRO, null, false));

        // When
        List<Instituicao> ativas = repository.findAllAtivas();

        // Then
        assertThat(ativas).hasSize(2);
        assertThat(ativas).allMatch(Instituicao::isAtiva);
    }

    @Test
    void findAllAtivasRetornaVazioQuandoNenhumaAtiva() {
        // Given
        repository.save(new Instituicao("Banco Inativo", null, TipoInstituicao.OUTRO, null, false));

        // When
        List<Instituicao> ativas = repository.findAllAtivas();

        // Then
        assertThat(ativas).isEmpty();
    }

    @Test
    void savePreservaCamposNullaveisCodigoBancoELogoUrl() {
        // Given
        Instituicao nova = new Instituicao("XP Investimentos", null, TipoInstituicao.CORRETORA, null, true);

        // When
        Instituicao salva = repository.save(nova);

        // Then
        assertThat(salva.getCodigoBanco()).isNull();
        assertThat(salva.getLogoUrl()).isNull();
    }
}
