package com.laboratorio.financas.usuario.interfaces;

import com.laboratorio.financas.usuario.application.LoginUseCase;
import com.laboratorio.financas.usuario.application.RegistrarUsuarioUseCase;
import com.laboratorio.financas.usuario.domain.CredenciaisInvalidasException;
import com.laboratorio.financas.usuario.domain.EmailJaExisteException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrarUsuarioUseCase registrarUseCase;
    private final LoginUseCase loginUseCase;

    public AuthController(RegistrarUsuarioUseCase registrarUseCase, LoginUseCase loginUseCase) {
        this.registrarUseCase = registrarUseCase;
        this.loginUseCase = loginUseCase;
    }

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> registrar(@RequestBody @Valid RegistrarRequest request) {
        try {
            var usuario = registrarUseCase.executar(
                    new RegistrarUsuarioUseCase.Comando(request.email(), request.senha()));
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(UsuarioResponse.fromDomain(usuario));
        } catch (EmailJaExisteException ex) {
            ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
            problem.setTitle("Conflict");
            problem.setDetail(ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {
        try {
            LoginUseCase.Resultado resultado = loginUseCase.executar(request.email(), request.senha());
            return ResponseEntity.ok(new TokenResponse(resultado.token(), resultado.tipo(), resultado.expiresIn()));
        } catch (CredenciaisInvalidasException ex) {
            ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
            problem.setTitle("Unauthorized");
            problem.setDetail(ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
        }
    }
}
