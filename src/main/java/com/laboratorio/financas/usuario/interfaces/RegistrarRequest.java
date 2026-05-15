package com.laboratorio.financas.usuario.interfaces;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String senha,
        @Size(max = 100) String name
) { }
