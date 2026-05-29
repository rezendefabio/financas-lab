package com.laboratorio.financas.limite.interfaces;

import com.laboratorio.financas.limite.application.AtualizarLimiteUseCase;
import com.laboratorio.financas.limite.application.BuscarLimiteUseCase;
import com.laboratorio.financas.limite.application.CriarLimiteUseCase;
import com.laboratorio.financas.limite.application.DesativarLimiteUseCase;
import com.laboratorio.financas.limite.application.ListarLimitesUseCase;
import com.laboratorio.financas.limite.interfaces.dto.AtualizarLimiteRequest;
import com.laboratorio.financas.limite.interfaces.dto.CriarLimiteRequest;
import com.laboratorio.financas.limite.interfaces.dto.LimiteResponse;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import jakarta.validation.Valid;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/limites")
public class LimiteController {

    private static final Currency MOEDA_PADRAO = Currency.getInstance("BRL");

    private final CriarLimiteUseCase criarUseCase;
    private final ListarLimitesUseCase listarUseCase;
    private final BuscarLimiteUseCase buscarUseCase;
    private final AtualizarLimiteUseCase atualizarUseCase;
    private final DesativarLimiteUseCase desativarUseCase;
    private final UsuarioRepository usuarioRepository;

    public LimiteController(
            CriarLimiteUseCase criarUseCase,
            ListarLimitesUseCase listarUseCase,
            BuscarLimiteUseCase buscarUseCase,
            AtualizarLimiteUseCase atualizarUseCase,
            DesativarLimiteUseCase desativarUseCase,
            UsuarioRepository usuarioRepository) {
        this.criarUseCase = criarUseCase;
        this.listarUseCase = listarUseCase;
        this.buscarUseCase = buscarUseCase;
        this.atualizarUseCase = atualizarUseCase;
        this.desativarUseCase = desativarUseCase;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public List<LimiteResponse> listar(Authentication authentication) {
        UUID userId = resolverUserId(authentication);
        return listarUseCase.executar(userId).stream()
                .map(LimiteResponse::fromDomain)
                .toList();
    }

    @GetMapping("/{id}")
    public LimiteResponse buscar(@PathVariable UUID id, Authentication authentication) {
        UUID userId = resolverUserId(authentication);
        return LimiteResponse.fromDomain(buscarUseCase.executar(id, userId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LimiteResponse criar(
            @Valid @RequestBody CriarLimiteRequest request,
            Authentication authentication) {
        UUID userId = resolverUserId(authentication);
        CriarLimiteUseCase.Comando comando = new CriarLimiteUseCase.Comando(
                userId,
                request.nome(),
                request.tipo(),
                new Money(request.valor(), MOEDA_PADRAO));
        return LimiteResponse.fromDomain(criarUseCase.executar(comando));
    }

    @PutMapping("/{id}")
    public LimiteResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarLimiteRequest request,
            Authentication authentication) {
        UUID userId = resolverUserId(authentication);
        AtualizarLimiteUseCase.Comando comando = new AtualizarLimiteUseCase.Comando(
                id,
                userId,
                request.nome(),
                request.tipo(),
                new Money(request.valor(), MOEDA_PADRAO));
        return LimiteResponse.fromDomain(atualizarUseCase.executar(comando));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable UUID id, Authentication authentication) {
        UUID userId = resolverUserId(authentication);
        desativarUseCase.executar(id, userId);
    }

    private UUID resolverUserId(Authentication authentication) {
        String email = authentication.getName();
        return usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Usuario autenticado nao encontrado: " + email))
                .getId();
    }
}
