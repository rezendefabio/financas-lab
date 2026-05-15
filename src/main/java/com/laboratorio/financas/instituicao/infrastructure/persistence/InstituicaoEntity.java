package com.laboratorio.financas.instituicao.infrastructure.persistence;

import com.laboratorio.financas.instituicao.domain.TipoInstituicao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "instituicao")
public class InstituicaoEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "codigo_banco", length = 10)
    private String codigoBanco;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoInstituicao tipo;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @NotNull
    @Column(name = "ativa", nullable = false)
    private boolean ativa;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected InstituicaoEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public InstituicaoEntity(
            UUID id,
            String nome,
            String codigoBanco,
            TipoInstituicao tipo,
            String logoUrl,
            boolean ativa,
            Instant criadoEm
    ) {
        this.id = id;
        this.nome = nome;
        this.codigoBanco = codigoBanco;
        this.tipo = tipo;
        this.logoUrl = logoUrl;
        this.ativa = ativa;
        this.criadoEm = criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCodigoBanco() {
        return codigoBanco;
    }

    public TipoInstituicao getTipo() {
        return tipo;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
