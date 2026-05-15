package com.laboratorio.financas.tag.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.tag.domain.Tag;
import com.laboratorio.financas.tag.domain.TagRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListarTagsUseCaseTest {

    private TagRepository repository;
    private ListarTagsUseCase useCase;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(TagRepository.class);
        useCase = new ListarTagsUseCase(repository);
    }

    @Test
    void executarRetornaListaDeTags() {
        Tag t1 = new Tag(UUID.randomUUID(), USER_ID, "Essencial", null, Instant.now());
        Tag t2 = new Tag(UUID.randomUUID(), USER_ID, "Lazer", "#00FF00", Instant.now());
        when(repository.buscarPorUserId(USER_ID)).thenReturn(List.of(t1, t2));

        List<Tag> resultado = useCase.executar(USER_ID);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).contains(t1, t2);
    }

    @Test
    void executarRetornaListaVaziaQuandoNaoHaTags() {
        when(repository.buscarPorUserId(USER_ID)).thenReturn(List.of());

        List<Tag> resultado = useCase.executar(USER_ID);

        assertThat(resultado).isEmpty();
    }

    @Test
    void executarPassaUserIdCorretoParaRepositorio() {
        when(repository.buscarPorUserId(USER_ID)).thenReturn(List.of());

        useCase.executar(USER_ID);

        Mockito.verify(repository).buscarPorUserId(USER_ID);
    }
}
