package com.laboratorio.financas.usuario.application;

public interface TokenService {

    String gerarToken(String email);

    long getExpirationSeconds();
}
