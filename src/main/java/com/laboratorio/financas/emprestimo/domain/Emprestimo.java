package com.laboratorio.financas.emprestimo.domain;

import com.laboratorio.financas.shared.domain.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class Emprestimo {

    private static final int DESCRICAO_MAX_LENGTH = 100;
    private static final int NOME_TERCEIRO_MAX_LENGTH = 100;

    private final UUID id;
    private final UUID userId;
    private String descricao;
    private String nomeTerceiro;
    private TipoEmprestimo tipo;
    private Money valor;
    private LocalDate dataEmprestimo;
    private boolean quitado;
    private final Instant criadoEm;
    private Instant atualizadoEm;

    public Emprestimo(UUID userId, String descricao, String nomeTerceiro,
                      TipoEmprestimo tipo, Money valor, LocalDate dataEmprestimo,
                      boolean quitado) {
        this(UUID.randomUUID(), userId, descricao, nomeTerceiro, tipo, valor,
                dataEmprestimo, quitado, Instant.now(), Instant.now());
    }

    public Emprestimo(UUID id, UUID userId, String descricao, String nomeTerceiro,
                      TipoEmprestimo tipo, Money valor, LocalDate dataEmprestimo,
                      boolean quitado, Instant criadoEm, Instant atualizadoEm) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(userId, "userId nao pode ser nulo");
        Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
        Objects.requireNonNull(valor, "valor nao pode ser nulo");
        Objects.requireNonNull(dataEmprestimo, "dataEmprestimo nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        validarDescricao(descricao);
        validarNomeTerceiro(nomeTerceiro);
        validarValor(valor);

        this.id = id;
        this.userId = userId;
        this.descricao = descricao.trim();
        this.nomeTerceiro = (nomeTerceiro != null && !nomeTerceiro.trim().isEmpty())
                ? nomeTerceiro.trim() : null;
        this.tipo = tipo;
        this.valor = valor;
        this.dataEmprestimo = dataEmprestimo;
        this.quitado = quitado;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    private static void validarDescricao(String descricao) {
        Objects.requireNonNull(descricao, "descricao nao pode ser nulo");
        String trimmed = descricao.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("descricao nao pode ser vazio");
        }
        if (trimmed.length() > DESCRICAO_MAX_LENGTH) {
            throw new IllegalArgumentException(
                "descricao nao pode ter mais de " + DESCRICAO_MAX_LENGTH + " caracteres");
        }
    }

    private static void validarNomeTerceiro(String nomeTerceiro) {
        if (nomeTerceiro != null && nomeTerceiro.trim().length() > NOME_TERCEIRO_MAX_LENGTH) {
            throw new IllegalArgumentException(
                "nomeTerceiro nao pode ter mais de " + NOME_TERCEIRO_MAX_LENGTH + " caracteres");
        }
    }

    private static void validarValor(Money valor) {
        if (!valor.ehPositivo()) {
            throw new IllegalArgumentException("valor deve ser positivo");
        }
    }

    public void atualizar(String descricao, String nomeTerceiro, TipoEmprestimo tipo,
                          Money valor, LocalDate dataEmprestimo, boolean quitado) {
        Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
        Objects.requireNonNull(valor, "valor nao pode ser nulo");
        Objects.requireNonNull(dataEmprestimo, "dataEmprestimo nao pode ser nulo");
        validarDescricao(descricao);
        validarNomeTerceiro(nomeTerceiro);
        validarValor(valor);

        this.descricao = descricao.trim();
        this.nomeTerceiro = (nomeTerceiro != null && !nomeTerceiro.trim().isEmpty())
                ? nomeTerceiro.trim() : null;
        this.tipo = tipo;
        this.valor = valor;
        this.dataEmprestimo = dataEmprestimo;
        this.quitado = quitado;
        this.atualizadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getNomeTerceiro() {
        return nomeTerceiro;
    }

    public TipoEmprestimo getTipo() {
        return tipo;
    }

    public Money getValor() {
        return valor;
    }

    public LocalDate getDataEmprestimo() {
        return dataEmprestimo;
    }

    public boolean isQuitado() {
        return quitado;
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
        if (!(o instanceof Emprestimo other)) {
            return false;
        }
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
