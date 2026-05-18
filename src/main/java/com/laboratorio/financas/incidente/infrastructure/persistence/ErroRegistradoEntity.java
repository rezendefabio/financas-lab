package com.laboratorio.financas.incidente.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "erro_registrado")
public class ErroRegistradoEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "codigo", nullable = false, length = 12, unique = true, updatable = false)
    private String codigo;

    @Column(name = "operacao", length = 100)
    private String operacao;

    @Column(name = "classe_erro", length = 200)
    private String classeErro;

    @Column(name = "mensagem", length = 500)
    private String mensagem;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected ErroRegistradoEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public ErroRegistradoEntity(
            UUID id,
            String codigo,
            String operacao,
            String classeErro,
            String mensagem,
            String stackTrace,
            Instant criadoEm
    ) {
        this.id = id;
        this.codigo = codigo;
        this.operacao = operacao;
        this.classeErro = classeErro;
        this.mensagem = mensagem;
        this.stackTrace = stackTrace;
        this.criadoEm = criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getOperacao() {
        return operacao;
    }

    public String getClasseErro() {
        return classeErro;
    }

    public String getMensagem() {
        return mensagem;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
