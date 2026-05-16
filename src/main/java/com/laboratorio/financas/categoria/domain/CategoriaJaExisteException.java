package com.laboratorio.financas.categoria.domain;

public class CategoriaJaExisteException extends RuntimeException {

    public CategoriaJaExisteException(String nome) {
        super("Ja existe uma categoria com o nome: " + nome);
    }
}
