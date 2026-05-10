package com.laboratorio.financas.conta.interfaces;

import com.laboratorio.financas.conta.application.BuscarContaPorIdUseCase;
import com.laboratorio.financas.conta.application.CalcularSaldoDaContaUseCase;
import com.laboratorio.financas.conta.application.CriarContaUseCase;
import com.laboratorio.financas.conta.application.DesativarContaUseCase;
import com.laboratorio.financas.conta.application.ListarContasUseCase;
import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.interfaces.dto.ContaResponse;
import com.laboratorio.financas.conta.interfaces.dto.CriarContaRequest;
import com.laboratorio.financas.conta.interfaces.dto.SaldoResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contas")
public class ContaController {

    private final CriarContaUseCase criarContaUseCase;
    private final ListarContasUseCase listarContasUseCase;
    private final BuscarContaPorIdUseCase buscarContaPorIdUseCase;
    private final DesativarContaUseCase desativarContaUseCase;
    private final CalcularSaldoDaContaUseCase calcularSaldoDaContaUseCase;

    public ContaController(
            CriarContaUseCase criarContaUseCase,
            ListarContasUseCase listarContasUseCase,
            BuscarContaPorIdUseCase buscarContaPorIdUseCase,
            DesativarContaUseCase desativarContaUseCase,
            CalcularSaldoDaContaUseCase calcularSaldoDaContaUseCase
    ) {
        this.criarContaUseCase = criarContaUseCase;
        this.listarContasUseCase = listarContasUseCase;
        this.buscarContaPorIdUseCase = buscarContaPorIdUseCase;
        this.desativarContaUseCase = desativarContaUseCase;
        this.calcularSaldoDaContaUseCase = calcularSaldoDaContaUseCase;
    }

    @PostMapping
    public ResponseEntity<ContaResponse> criar(@Valid @RequestBody CriarContaRequest request) {
        CriarContaUseCase.Comando comando = new CriarContaUseCase.Comando(
                request.nome(),
                request.tipo(),
                request.saldoInicialValor(),
                request.saldoInicialMoeda()
        );
        Conta criada = criarContaUseCase.executar(comando);
        return ResponseEntity.status(HttpStatus.CREATED).body(ContaResponse.fromDomain(criada));
    }

    @GetMapping
    public List<ContaResponse> listar(@RequestParam(name = "ativa", required = false) Boolean ativa) {
        boolean apenasAtivas = Boolean.TRUE.equals(ativa);
        List<Conta> contas = listarContasUseCase.executar(apenasAtivas);
        return contas.stream().map(ContaResponse::fromDomain).toList();
    }

    @GetMapping("/{id}")
    public ContaResponse buscar(@PathVariable UUID id) {
        Conta conta = buscarContaPorIdUseCase.executar(id);
        return ContaResponse.fromDomain(conta);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable UUID id) {
        desativarContaUseCase.executar(id);
    }

    @GetMapping("/{id}/saldo")
    public SaldoResponse calcularSaldo(@PathVariable UUID id) {
        CalcularSaldoDaContaUseCase.Resultado resultado = calcularSaldoDaContaUseCase.executar(id);
        return SaldoResponse.fromResultado(resultado);
    }
}
