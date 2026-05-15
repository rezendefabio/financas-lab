package com.laboratorio.financas.payee.interfaces;

import com.laboratorio.financas.payee.application.AtualizarPayeeUseCase;
import com.laboratorio.financas.payee.application.CriarPayeeUseCase;
import com.laboratorio.financas.payee.application.DeletarPayeeUseCase;
import com.laboratorio.financas.payee.application.ListarPayeesUseCase;
import com.laboratorio.financas.payee.application.dto.AtualizarPayeeComando;
import com.laboratorio.financas.payee.application.dto.CriarPayeeComando;
import com.laboratorio.financas.payee.domain.Payee;
import com.laboratorio.financas.usuario.domain.Usuario;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/payees")
public class PayeeController {

    private final CriarPayeeUseCase criarPayeeUseCase;
    private final ListarPayeesUseCase listarPayeesUseCase;
    private final AtualizarPayeeUseCase atualizarPayeeUseCase;
    private final DeletarPayeeUseCase deletarPayeeUseCase;
    private final UsuarioRepository usuarioRepository;

    public PayeeController(
            CriarPayeeUseCase criarPayeeUseCase,
            ListarPayeesUseCase listarPayeesUseCase,
            AtualizarPayeeUseCase atualizarPayeeUseCase,
            DeletarPayeeUseCase deletarPayeeUseCase,
            UsuarioRepository usuarioRepository
    ) {
        this.criarPayeeUseCase = criarPayeeUseCase;
        this.listarPayeesUseCase = listarPayeesUseCase;
        this.atualizarPayeeUseCase = atualizarPayeeUseCase;
        this.deletarPayeeUseCase = deletarPayeeUseCase;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public List<PayeeResponse> listar() {
        UUID userId = resolverUserId();
        List<Payee> payees = listarPayeesUseCase.executar(userId);
        return payees.stream().map(PayeeResponse::fromDomain).toList();
    }

    @PostMapping
    public ResponseEntity<PayeeResponse> criar(@Valid @RequestBody PayeeRequest request) {
        UUID userId = resolverUserId();
        CriarPayeeComando comando = new CriarPayeeComando(userId, request.nome(), request.categoriaPadraoId());
        Payee criado = criarPayeeUseCase.executar(comando);
        return ResponseEntity.status(HttpStatus.CREATED).body(PayeeResponse.fromDomain(criado));
    }

    @PutMapping("/{id}")
    public PayeeResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody PayeeRequest request
    ) {
        UUID userId = resolverUserId();
        AtualizarPayeeComando comando = new AtualizarPayeeComando(
                id,
                userId,
                request.nome(),
                request.categoriaPadraoId()
        );
        Payee atualizado = atualizarPayeeUseCase.executar(comando);
        return PayeeResponse.fromDomain(atualizado);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable UUID id) {
        UUID userId = resolverUserId();
        deletarPayeeUseCase.executar(id, userId);
    }

    private UUID resolverUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (String) auth.getPrincipal();
        Usuario usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado nao encontrado: " + email));
        return usuario.getId();
    }
}
