package com.laboratorio.financas.tag.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.tag.domain.Tag;
import com.laboratorio.financas.tag.domain.TagRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CriarTagUseCaseTest {

    private TagRepository repository;
    private CriarTagUseCase useCase;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(TagRepository.class);
        useCase = new CriarTagUseCase(repository);
    }

    @Test
    void executarCaminhoFelizRetornaTagCriada() {
        Tag tagSalva = new Tag(USER_ID, "Essencial", "#FF0000");
        when(repository.buscarPorUserId(USER_ID)).thenReturn(List.of());
        when(repository.salvar(any(Tag.class))).thenReturn(tagSalva);

        CriarTagUseCase.Comando comando = new CriarTagUseCase.Comando(USER_ID, "Essencial", "#FF0000");
        Tag resultado = useCase.executar(comando);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNome()).isEqualTo("Essencial");
    }

    @Test
    void executarChamaRepositorioSaveUmaVez() {
        Tag tagSalva = new Tag(USER_ID, "Lazer", null);
        when(repository.buscarPorUserId(USER_ID)).thenReturn(List.of());
        when(repository.salvar(any(Tag.class))).thenReturn(tagSalva);

        useCase.executar(new CriarTagUseCase.Comando(USER_ID, "Lazer", null));

        verify(repository, times(1)).salvar(any(Tag.class));
    }

    @Test
    void executarComNomeDuplicadoLancaIllegalArgumentException() {
        UUID existenteId = UUID.randomUUID();
        Tag existente = new Tag(existenteId, USER_ID, "Essencial", null, Instant.now());
        when(repository.buscarPorUserId(USER_ID)).thenReturn(List.of(existente));

        CriarTagUseCase.Comando comando = new CriarTagUseCase.Comando(USER_ID, "Essencial", null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> useCase.executar(comando))
                .withMessageContaining("Essencial");

        verify(repository, never()).salvar(any(Tag.class));
    }

    @Test
    void executarComNomeDuplicadoIgnorandoCasoLancaIllegalArgumentException() {
        UUID existenteId = UUID.randomUUID();
        Tag existente = new Tag(existenteId, USER_ID, "essencial", null, Instant.now());
        when(repository.buscarPorUserId(USER_ID)).thenReturn(List.of(existente));

        CriarTagUseCase.Comando comando = new CriarTagUseCase.Comando(USER_ID, "ESSENCIAL", null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> useCase.executar(comando));
    }

    @Test
    void executarRetornaOQueRepositorioRetornou() {
        Tag tagSalva = new Tag(USER_ID, "Viagem", "#0000FF");
        when(repository.buscarPorUserId(USER_ID)).thenReturn(List.of());
        when(repository.salvar(any(Tag.class))).thenReturn(tagSalva);

        Tag resultado = useCase.executar(new CriarTagUseCase.Comando(USER_ID, "Viagem", "#0000FF"));

        assertThat(resultado).isSameAs(tagSalva);
    }
}
