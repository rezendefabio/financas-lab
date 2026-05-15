package com.laboratorio.financas.tag.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

class AtualizarTagUseCaseTest {

    private TagRepository repository;
    private AtualizarTagUseCase useCase;

    private static final UUID TAG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(TagRepository.class);
        useCase = new AtualizarTagUseCase(repository);
    }

    @Test
    void executarAtualizaNomeECor() {
        Tag existente = new Tag(TAG_ID, USER_ID, "Velha", "#000000", Instant.now());
        Tag atualizada = new Tag(TAG_ID, USER_ID, "Nova", "#FFFFFF", Instant.now());
        when(repository.findByIdAndUserId(TAG_ID, USER_ID)).thenReturn(Optional.of(existente));
        when(repository.save(any(Tag.class))).thenReturn(atualizada);

        AtualizarTagUseCase.Comando comando = new AtualizarTagUseCase.Comando(TAG_ID, USER_ID, "Nova", "#FFFFFF");
        Tag resultado = useCase.executar(comando);

        assertThat(resultado.getNome()).isEqualTo("Nova");
        assertThat(resultado.getCor()).isEqualTo("#FFFFFF");
    }

    @Test
    void executarComNomeNuloMantemNomeExistente() {
        Tag existente = new Tag(TAG_ID, USER_ID, "Essencial", "#FF0000", Instant.now());
        when(repository.findByIdAndUserId(TAG_ID, USER_ID)).thenReturn(Optional.of(existente));
        when(repository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        AtualizarTagUseCase.Comando comando = new AtualizarTagUseCase.Comando(TAG_ID, USER_ID, null, "#00FF00");
        Tag resultado = useCase.executar(comando);

        assertThat(resultado.getNome()).isEqualTo("Essencial");
    }

    @Test
    void executarComCorNulaMantemCorExistente() {
        Tag existente = new Tag(TAG_ID, USER_ID, "Essencial", "#FF0000", Instant.now());
        when(repository.findByIdAndUserId(TAG_ID, USER_ID)).thenReturn(Optional.of(existente));
        when(repository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        AtualizarTagUseCase.Comando comando = new AtualizarTagUseCase.Comando(TAG_ID, USER_ID, "Nova", null);
        Tag resultado = useCase.executar(comando);

        assertThat(resultado.getCor()).isEqualTo("#FF0000");
    }

    @Test
    void executarTagNaoEncontradaLancaException() {
        when(repository.findByIdAndUserId(TAG_ID, USER_ID)).thenReturn(Optional.empty());

        AtualizarTagUseCase.Comando comando = new AtualizarTagUseCase.Comando(TAG_ID, USER_ID, "Nova", null);

        assertThatThrownBy(() -> useCase.executar(comando))
                .isInstanceOf(TagNaoEncontradaException.class);

        verify(repository, never()).save(any(Tag.class));
    }
}
