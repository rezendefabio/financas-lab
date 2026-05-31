package com.laboratorio.financas.notificacao.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Notificacao materializada para um usuario.
 *
 * <p>As notificacoes sao derivadas do estado de orcamentos e metas (calculadas
 * pela reconciliacao), mas persistidas para que o descarte ({@link #descartar()})
 * sobreviva entre logins -- esse e o ponto central: sem persistencia, o descarte
 * vivia so no estado local do frontend e a notificacao reaparecia.
 *
 * <p>Chave natural: {@code (userId, tipo, referenciaId)}. O {@code referenciaId}
 * e o id do orcamento ou da meta que originou a notificacao. Como o orcamento e
 * por mes (mesAno), a chave e period-scoped: descartar a deste mes nao silencia
 * a do proximo (orcamento diferente).
 */
public final class Notificacao {

    private final UUID id;
    private final UUID userId;
    private final TipoNotificacao tipo;
    private final UUID referenciaId;
    private String titulo;
    private String descricao;
    private boolean descartada;
    private final Instant criadoEm;
    private Instant atualizadoEm;

    /** Construtor de criacao: gera id, descartada=false, timestamps=now. */
    public Notificacao(UUID userId, TipoNotificacao tipo, UUID referenciaId,
                       String titulo, String descricao) {
        this(UUID.randomUUID(), userId, tipo, referenciaId, titulo, descricao,
                false, Instant.now(), Instant.now());
    }

    /** Construtor de reconstrucao: todos os campos (mapper). */
    public Notificacao(UUID id, UUID userId, TipoNotificacao tipo, UUID referenciaId,
                       String titulo, String descricao, boolean descartada,
                       Instant criadoEm, Instant atualizadoEm) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(userId, "userId nao pode ser nulo");
        Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
        Objects.requireNonNull(referenciaId, "referenciaId nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        validarTexto(titulo, descricao);

        this.id = id;
        this.userId = userId;
        this.tipo = tipo;
        this.referenciaId = referenciaId;
        this.titulo = titulo;
        this.descricao = descricao;
        this.descartada = descartada;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    private static void validarTexto(String titulo, String descricao) {
        Objects.requireNonNull(titulo, "titulo nao pode ser nulo");
        Objects.requireNonNull(descricao, "descricao nao pode ser nula");
        if (titulo.isBlank()) {
            throw new IllegalArgumentException("titulo nao pode ser vazio");
        }
    }

    /** Marca como descartada. Persiste para nao reaparecer no proximo login. */
    public void descartar() {
        this.descartada = true;
        this.atualizadoEm = Instant.now();
    }

    /**
     * Atualiza titulo/descricao quando o estado de origem mudou (ex: o percentual
     * do orcamento subiu). Preserva o flag {@code descartada}.
     */
    public void atualizarTexto(String novoTitulo, String novaDescricao) {
        validarTexto(novoTitulo, novaDescricao);
        boolean mudou = !Objects.equals(this.titulo, novoTitulo)
                || !Objects.equals(this.descricao, novaDescricao);
        if (mudou) {
            this.titulo = novoTitulo;
            this.descricao = novaDescricao;
            this.atualizadoEm = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public TipoNotificacao getTipo() {
        return tipo;
    }

    public UUID getReferenciaId() {
        return referenciaId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isDescartada() {
        return descartada;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Notificacao other)) {
            return false;
        }
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
