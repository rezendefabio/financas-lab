package com.laboratorio.financas.usuario.interfaces.dto;

import jakarta.validation.constraints.Size;

public record AtualizarPerfilRequest(
        @Size(max = 100) String name
) { }
