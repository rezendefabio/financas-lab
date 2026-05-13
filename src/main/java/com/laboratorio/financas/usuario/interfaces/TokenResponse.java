package com.laboratorio.financas.usuario.interfaces;

public record TokenResponse(String token, String tipo, long expiresIn) { }
