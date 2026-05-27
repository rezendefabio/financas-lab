package com.laboratorio.financas.usuario.interfaces;

import com.laboratorio.financas.usuario.application.AlterarSenhaUseCase;
import com.laboratorio.financas.usuario.application.AtualizarPerfilUseCase;
import com.laboratorio.financas.usuario.application.BuscarPerfilUseCase;
import com.laboratorio.financas.usuario.domain.SenhaInvalidaException;
import com.laboratorio.financas.usuario.interfaces.dto.AlterarSenhaRequest;
import com.laboratorio.financas.usuario.interfaces.dto.AtualizarPerfilRequest;
import com.laboratorio.financas.usuario.interfaces.dto.PerfilResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final BuscarPerfilUseCase buscarUseCase;
    private final AtualizarPerfilUseCase atualizarUseCase;
    private final AlterarSenhaUseCase alterarSenhaUseCase;

    public PerfilController(BuscarPerfilUseCase buscarUseCase,
                            AtualizarPerfilUseCase atualizarUseCase,
                            AlterarSenhaUseCase alterarSenhaUseCase) {
        this.buscarUseCase = buscarUseCase;
        this.atualizarUseCase = atualizarUseCase;
        this.alterarSenhaUseCase = alterarSenhaUseCase;
    }

    private String emailDoContexto() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping
    public PerfilResponse buscar() {
        return PerfilResponse.fromDomain(buscarUseCase.executar(emailDoContexto()));
    }

    @PutMapping
    public PerfilResponse atualizar(@RequestBody @Valid AtualizarPerfilRequest req) {
        return PerfilResponse.fromDomain(
                atualizarUseCase.executar(new AtualizarPerfilUseCase.Comando(emailDoContexto(), req.name()))
        );
    }

    @PutMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarSenha(@RequestBody @Valid AlterarSenhaRequest req) {
        alterarSenhaUseCase.executar(
                new AlterarSenhaUseCase.Comando(emailDoContexto(), req.senhaAtual(), req.novaSenha())
        );
    }

    @ExceptionHandler(SenhaInvalidaException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ProblemDetail handleSenhaInvalida(SenhaInvalidaException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problem.setTitle("Unprocessable Entity");
        problem.setDetail(ex.getMessage());
        return problem;
    }
}
