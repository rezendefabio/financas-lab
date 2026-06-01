package com.laboratorio.financas.centrocusto.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.laboratorio.financas.centrocusto.domain.CentroCusto;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioEntity;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class CentroCustoRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private CentroCustoRepositoryImpl repository;

    @Autowired
    private CentroCustoJpaRepository jpaRepository;

    @Autowired
    private UsuarioJpaRepository usuarioJpaRepository;

    private UUID userId;
    private UUID outroUserId;

    @BeforeEach
    void limparAntes() {
        jpaRepository.deleteAll();
        usuarioJpaRepository.deleteAll();
        userId = criarUsuarioPersistido();
        outroUserId = criarUsuarioPersistido();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
        usuarioJpaRepository.deleteAll();
    }

    private UUID criarUsuarioPersistido() {
        UUID id = UUID.randomUUID();
        UsuarioEntity entity = new UsuarioEntity(
                id,
                "teste+" + id + "@test.com",
                "hash_bcrypt",
                true,
                Instant.now(),
                null,
                Instant.now()
        );
        usuarioJpaRepository.save(entity);
        return id;
    }

    @Test
    void savePersisteERetornaInstanciaEquivalente() {
        CentroCusto novo = new CentroCusto(userId, "Casa", "Despesas residenciais");

        CentroCusto salvo = repository.save(novo);

        assertThat(salvo.getId()).isEqualTo(novo.getId());
        assertThat(salvo.getUserId()).isEqualTo(userId);
        assertThat(salvo.getNome()).isEqualTo("Casa");
        assertThat(salvo.getDescricao()).isEqualTo("Despesas residenciais");
        assertThat(salvo.isAtivo()).isTrue();
    }

    @Test
    void findByIdRetornaCentroCustoQuandoExiste() {
        CentroCusto novo = new CentroCusto(userId, "Trabalho", null);
        repository.save(novo);

        Optional<CentroCusto> resultado = repository.findById(novo.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("Trabalho");
    }

    @Test
    void listarTodosRetornaCentrosDeQualquerUsuario() {
        repository.save(new CentroCusto(userId, "Casa", null));
        repository.save(new CentroCusto(userId, "Trabalho", null));
        repository.save(new CentroCusto(outroUserId, "Casa", null));

        List<CentroCusto> resultado = repository.listarTodos();

        assertThat(resultado).hasSize(3);
        assertThat(resultado).anyMatch(c -> c.getUserId().equals(userId));
        assertThat(resultado).anyMatch(c -> c.getUserId().equals(outroUserId));
    }

    @Test
    void findByIdAndUserIdRetornaVazioQuandoUserIdDiferente() {
        CentroCusto novo = new CentroCusto(userId, "Casa", null);
        repository.save(novo);

        Optional<CentroCusto> resultado = repository.findByIdAndUserId(novo.getId(), outroUserId);

        assertThat(resultado).isEmpty();
    }

    @Test
    void findByIdAndUserIdRetornaQuandoCasaUsuario() {
        CentroCusto novo = new CentroCusto(userId, "Casa", null);
        repository.save(novo);

        Optional<CentroCusto> resultado = repository.findByIdAndUserId(novo.getId(), userId);

        assertThat(resultado).isPresent();
    }

    @Test
    void uniqueIndexCaseInsensitiveBloqueiaNomeDuplicado() {
        repository.save(new CentroCusto(userId, "Casa", null));

        CentroCusto duplicado = new CentroCusto(userId, "CASA", null);

        assertThatThrownBy(() -> {
            repository.save(duplicado);
            jpaRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void uniqueIndexPermiteMesmoNomeEmUsuariosDiferentes() {
        repository.save(new CentroCusto(userId, "Casa", null));
        CentroCusto outro = repository.save(new CentroCusto(outroUserId, "Casa", null));

        assertThat(outro.getId()).isNotNull();
    }

    @Test
    void saveAtualizaCentroCustoExistente() {
        CentroCusto novo = new CentroCusto(userId, "Casa", null);
        repository.save(novo);

        CentroCusto desativado = novo.desativar();
        repository.save(desativado);

        Optional<CentroCusto> resultado = repository.findById(novo.getId());
        assertThat(resultado).isPresent();
        assertThat(resultado.get().isAtivo()).isFalse();
    }
}
