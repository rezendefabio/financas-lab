package com.laboratorio.financas.anexo.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "anexo")
public class AnexoEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    @NotNull
    @Column(name = "tipo_conteudo", nullable = false, length = 100)
    private String tipoConteudo;

    @Column(name = "tamanho", nullable = false)
    private long tamanho;

    @NotNull
    @Column(name = "chave_armazenamento", nullable = false, length = 500)
    private String chaveArmazenamento;

    @NotNull
    @Column(name = "entidade_tipo", nullable = false, length = 50)
    private String entidadeTipo;

    @NotNull
    @Column(name = "entidade_id", columnDefinition = "uuid", nullable = false)
    private UUID entidadeId;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected AnexoEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public AnexoEntity(
            UUID id,
            String nome,
            String tipoConteudo,
            long tamanho,
            String chaveArmazenamento,
            String entidadeTipo,
            UUID entidadeId,
            Instant criadoEm
    ) {
        this.id = id;
        this.nome = nome;
        this.tipoConteudo = tipoConteudo;
        this.tamanho = tamanho;
        this.chaveArmazenamento = chaveArmazenamento;
        this.entidadeTipo = entidadeTipo;
        this.entidadeId = entidadeId;
        this.criadoEm = criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getTipoConteudo() {
        return tipoConteudo;
    }

    public long getTamanho() {
        return tamanho;
    }

    public String getChaveArmazenamento() {
        return chaveArmazenamento;
    }

    public String getEntidadeTipo() {
        return entidadeTipo;
    }

    public UUID getEntidadeId() {
        return entidadeId;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
