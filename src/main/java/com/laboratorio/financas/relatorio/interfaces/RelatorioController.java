package com.laboratorio.financas.relatorio.interfaces;

import com.laboratorio.financas.relatorio.application.EvolucaoSaldoUseCase;
import com.laboratorio.financas.relatorio.application.FluxoCaixaUseCase;
import com.laboratorio.financas.relatorio.application.GastosPorCategoriaUseCase;
import com.laboratorio.financas.relatorio.interfaces.dto.EvolucaoSaldoResponse;
import com.laboratorio.financas.relatorio.interfaces.dto.GastosPorCategoriaResponse;
import com.laboratorio.financas.usuario.domain.Usuario;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final GastosPorCategoriaUseCase gastosPorCategoriaUseCase;
    private final EvolucaoSaldoUseCase evolucaoSaldoUseCase;
    private final FluxoCaixaUseCase fluxoCaixaUseCase;
    private final UsuarioRepository usuarioRepository;

    public RelatorioController(GastosPorCategoriaUseCase gastosPorCategoriaUseCase,
                               EvolucaoSaldoUseCase evolucaoSaldoUseCase,
                               FluxoCaixaUseCase fluxoCaixaUseCase,
                               UsuarioRepository usuarioRepository) {
        this.gastosPorCategoriaUseCase = gastosPorCategoriaUseCase;
        this.evolucaoSaldoUseCase = evolucaoSaldoUseCase;
        this.fluxoCaixaUseCase = fluxoCaixaUseCase;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/gastos-por-categoria")
    public GastosPorCategoriaResponse gastosPorCategoria(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) UUID contaId) {
        var resultado = gastosPorCategoriaUseCase.executar(
                new GastosPorCategoriaUseCase.Consulta(dataInicio, dataFim, contaId));
        return GastosPorCategoriaResponse.fromResultado(resultado);
    }

    @GetMapping("/evolucao-saldo")
    public EvolucaoSaldoResponse evolucaoSaldo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) UUID contaId) {
        var resultado = evolucaoSaldoUseCase.executar(
                new EvolucaoSaldoUseCase.Consulta(dataInicio, dataFim, contaId));
        return EvolucaoSaldoResponse.fromResultado(resultado);
    }

    @GetMapping("/dashboard/fluxo-caixa")
    public ResponseEntity<FluxoCaixaUseCase.FluxoCaixaResponse> fluxoCaixa(
            @RequestParam int ano,
            @RequestParam @Min(1) @Max(12) int mes) {
        UUID userId = resolverUserId();
        return ResponseEntity.ok(fluxoCaixaUseCase.executar(ano, mes, userId));
    }

    private UUID resolverUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (String) auth.getPrincipal();
        Usuario usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado nao encontrado: " + email));
        return usuario.getId();
    }
}
