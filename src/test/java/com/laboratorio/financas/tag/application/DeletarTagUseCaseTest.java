package com.laboratorio.financas.tag.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.tag.domain.Tag;
import com.laboratorio.financas.tag.domain.TagNaoEncontradaException;
import com.laboratorio.financas.tag.domain.TagRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DeletarTagUseCaseTest {

    private TagRepository repository;
    private DeletarTagUseCase useCase;

    private static final UUID TAG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(TagRepository.class);
        useCase = new DeletarTagUseCase(repository);
    }

    @Test
    void executarCaminhoFelizChamaDeleteById() {
        Tag tag = new Tag(TAG_ID, USER_ID, "Essencial", null, Instant.now());
        when(repository.buscarPorIdEUserId(TAG_ID, USER_ID)).thenReturn(Optional.of(tag));

        useCase.executar(TAG_ID, USER_ID);

        verify(repository, times(1)).deletar(TAG_ID);
    }

    @Test
    void executarTagNaoEncontradaLancaException() {
        when(repository.buscarPorIdEUserId(TAG_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(TAG_ID, USER_ID))
                .isInstanceOf(TagNaoEncontradaException.class);

        verify(repository, never()).deletar(TAG_ID);
    }

    @Test
    void executarNaoRemoveTagDeOutroUsuario() {
        UUID outroUserId = UUID.randomUUID();
        when(repository.buscarPorIdEUserId(TAG_ID, outroUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(TAG_ID, outroUserId))
                .isInstanceOf(TagNaoEncontradaException.class);

        verify(repository, never()).deletar(TAG_ID);
    }
}
