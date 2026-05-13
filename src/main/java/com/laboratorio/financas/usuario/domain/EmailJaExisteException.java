package com.laboratorio.financas.usuario.domain;

public class EmailJaExisteException extends RuntimeException {

    public EmailJaExisteException(String email) {
        super("Email ja cadastrado: " + email);
    }
}
