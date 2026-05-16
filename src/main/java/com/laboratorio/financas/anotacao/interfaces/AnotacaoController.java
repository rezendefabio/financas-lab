package com.laboratorio.financas.anotacao.interfaces;

import com.laboratorio.financas.anotacao.application.AtualizarAnotacaoUseCase;
import com.laboratorio.financas.anotacao.application.BuscarAnotacaoPorIdUseCase;
import com.laboratorio.financas.anotacao.application.CriarAnotacaoUseCase;
import com.laboratorio.financas.anotacao.application.DeletarAnotacaoUseCase;
import com.laboratorio.financas.anotacao.application.ListarAnotacoesUseCase;
import com.laboratorio.financas.anotacao.domain.Anotacao;
import com.laboratorio.financas.anotacao.interfaces.dto.AnotacaoResponse;
import com.laboratorio.financas.anotacao.interfaces.dto.AtualizarAnotacaoRequest;
import com.laboratorio.financas.anotacao.interfaces.dto.CriarAnotacaoRequest;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.usuario.domain.Usuario;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
@RequestMapping("/api/anotacoes")
public class AnotacaoController {

    private final CriarAnotacaoUseCase criarUseCase;
    private final ListarAnotacoesUseCase listarUseCase;
    private final BuscarAnotacaoPorIdUseCase buscarUseCase;
    private final AtualizarAnotacaoUseCase atualizarUseCase;
    private final DeletarAnotacaoUseCase deletarUseCase;
    private final UsuarioRepository usuarioRepository;

    public AnotacaoController(
            CriarAnotacaoUseCase criarUseCase,
            ListarAnotacoesUseCase listarUseCase,
            BuscarAnotacaoPorIdUseCase buscarUseCase,
            AtualizarAnotacaoUseCase atualizarUseCase,
            DeletarAnotacaoUseCase deletarUseCase,
            UsuarioRepository usuarioRepository
    ) {
        this.criarUseCase = criarUseCase;
        this.listarUseCase = listarUseCase;
        this.buscarUseCase = buscarUseCase;
        this.atualizarUseCase = atualizarUseCase;
        this.deletarUseCase = deletarUseCase;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnotacaoResponse criar(@Valid @RequestBody CriarAnotacaoRequest request) {
        UUID usuarioId = resolverUserId();
        Money valor = buildMoney(request.valorMontante(), request.valorMoeda());
        Anotacao anotacao = criarUseCase.executar(
                usuarioId,
                request.titulo(),
                request.conteudo(),
                request.tipo(),
                request.prioridade(),
                valor,
                request.dataReferencia()
        );
        return AnotacaoResponse.fromDomain(anotacao);
    }

    @GetMapping
    public List<AnotacaoResponse> listar() {
        UUID usuarioId = resolverUserId();
        return listarUseCase.executar(usuarioId).stream()
                .map(AnotacaoResponse::fromDomain)
                .toList();
    }

    @GetMapping("/{id}")
    public AnotacaoResponse buscar(@PathVariable UUID id) {
        return AnotacaoResponse.fromDomain(buscarUseCase.executar(id));
    }

    @PutMapping("/{id}")
    public AnotacaoResponse atualizar(@PathVariable UUID id,
                                       @Valid @RequestBody AtualizarAnotacaoRequest request) {
        Money valor = buildMoney(request.valorMontante(), request.valorMoeda());
        Anotacao anotacao = atualizarUseCase.executar(
                id,
                request.titulo(),
                request.conteudo(),
                request.tipo(),
                request.prioridade(),
                valor,
                request.dataReferencia()
        );
        return AnotacaoResponse.fromDomain(anotacao);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable UUID id) {
        deletarUseCase.executar(id);
    }

    private UUID resolverUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (String) auth.getPrincipal();
        Usuario usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado nao encontrado: " + email));
        return usuario.getId();
    }

    private Money buildMoney(BigDecimal montante, String moeda) {
        if (montante == null) {
            return null;
        }
        String moedaStr = (moeda != null && !moeda.isBlank()) ? moeda : "BRL";
        return new Money(montante, Currency.getInstance(moedaStr));
    }
}
