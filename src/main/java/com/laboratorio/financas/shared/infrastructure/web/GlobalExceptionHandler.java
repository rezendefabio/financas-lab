package com.laboratorio.financas.shared.infrastructure.web;

import com.laboratorio.financas.anexo.domain.AnexoNaoEncontradoException;
import com.laboratorio.financas.anotacao.domain.AnotacaoNaoEncontradaException;
import com.laboratorio.financas.carteira.domain.CarteiraNaoEncontradaException;
import com.laboratorio.financas.categoria.domain.CategoriaJaExisteException;
import com.laboratorio.financas.categoria.domain.CategoriaNaoEncontradaException;
import com.laboratorio.financas.centrocusto.domain.CentroCustoNaoEncontradoException;
import com.laboratorio.financas.conta.domain.ContaNaoEncontradaException;
import com.laboratorio.financas.fatura.domain.FaturaNaoEncontradaException;
import com.laboratorio.financas.grupo.domain.GrupoNaoEncontradoException;
import com.laboratorio.financas.incidente.application.RegistrarErroUseCase;
import com.laboratorio.financas.incidente.domain.IncidenteNaoEncontradoException;
import com.laboratorio.financas.instituicao.domain.InstituicaoNaoEncontradaException;
import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrenteNaoEncontradoException;
import com.laboratorio.financas.meta.domain.MetaNaoEncontradaException;
import com.laboratorio.financas.orcamento.domain.OrcamentoNaoEncontradoException;
import com.laboratorio.financas.payee.domain.PayeeNaoEncontradoException;
import com.laboratorio.financas.tag.domain.TagNaoEncontradaException;
import com.laboratorio.financas.transacao.domain.TransacaoComReferenciaInvalidaException;
import com.laboratorio.financas.transacao.domain.TransacaoNaoEncontradaException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final RegistrarErroUseCase registrarErroUseCase;

    public GlobalExceptionHandler(RegistrarErroUseCase registrarErroUseCase) {
        this.registrarErroUseCase = registrarErroUseCase;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Bad Request");
        problem.setDetail("Validacao falhou em um ou mais campos.");
        Map<String, String> erros = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                erros.put(fieldError.getField(), fieldError.getDefaultMessage())
        );
        problem.setProperty("erros", erros);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleJsonMalformado(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Bad Request");
        problem.setDetail("Corpo da requisicao invalido ou malformado.");
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Bad Request");
        problem.setDetail("Parametro '" + ex.getName() + "' tem formato invalido.");
        return problem;
    }

    @ExceptionHandler(ContaNaoEncontradaException.class)
    public ProblemDetail handleContaNaoEncontrada(ContaNaoEncontradaException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("id", ex.getId().toString());
        return problem;
    }

    @ExceptionHandler(CategoriaNaoEncontradaException.class)
    public ProblemDetail handleCategoriaNaoEncontrada(CategoriaNaoEncontradaException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("id", ex.getId().toString());
        return problem;
    }

    @ExceptionHandler(CategoriaJaExisteException.class)
    public ProblemDetail handleCategoriaJaExiste(CategoriaJaExisteException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Conflict");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(OrcamentoNaoEncontradoException.class)
    public ProblemDetail handleOrcamentoNaoEncontrado(OrcamentoNaoEncontradoException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("id", ex.getId().toString());
        return problem;
    }

    @ExceptionHandler(TransacaoNaoEncontradaException.class)
    public ProblemDetail handleTransacaoNaoEncontrada(TransacaoNaoEncontradaException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("id", ex.getId().toString());
        return problem;
    }

    @ExceptionHandler(TransacaoComReferenciaInvalidaException.class)
    public ProblemDetail handleReferenciaInvalida(TransacaoComReferenciaInvalidaException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Bad Request");
        problem.setDetail(ex.getMessage());
        problem.setProperty("recurso", ex.getRecurso());
        problem.setProperty("id", ex.getId().toString());
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Bad Request");
        problem.setDetail("Parametro fora dos limites permitidos.");
        problem.setProperty("violacoes", ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList());
        return problem;
    }

    @ExceptionHandler(MetaNaoEncontradaException.class)
    public ProblemDetail handleMetaNaoEncontrada(MetaNaoEncontradaException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("id", ex.getId().toString());
        return problem;
    }

    @ExceptionHandler(InstituicaoNaoEncontradaException.class)
    public ProblemDetail handleInstituicaoNaoEncontrada(InstituicaoNaoEncontradaException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("id", ex.getId().toString());
        return problem;
    }

    @ExceptionHandler(LancamentoRecorrenteNaoEncontradoException.class)
    public ProblemDetail handleLancamentoRecorrenteNaoEncontrado(
            LancamentoRecorrenteNaoEncontradoException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("id", ex.getId().toString());
        return problem;
    }

    @ExceptionHandler(CentroCustoNaoEncontradoException.class)
    public ProblemDetail handleCentroCustoNaoEncontrado(CentroCustoNaoEncontradoException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("id", ex.getId().toString());
        return problem;
    }

    @ExceptionHandler(PayeeNaoEncontradoException.class)
    public ProblemDetail handlePayeeNaoEncontrado(PayeeNaoEncontradoException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("id", ex.getId().toString());
        return problem;
    }

    @ExceptionHandler(TagNaoEncontradaException.class)
    public ProblemDetail handleTagNaoEncontrada(TagNaoEncontradaException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("id", ex.getId().toString());
        return problem;
    }

    @ExceptionHandler(AnotacaoNaoEncontradaException.class)
    public ProblemDetail handleAnotacaoNaoEncontrada(AnotacaoNaoEncontradaException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("id", ex.getId().toString());
        return problem;
    }

    @ExceptionHandler(AnexoNaoEncontradoException.class)
    public ProblemDetail handleAnexoNaoEncontrado(AnexoNaoEncontradoException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("id", ex.getId().toString());
        return problem;
    }

    @ExceptionHandler(FaturaNaoEncontradaException.class)
    public ProblemDetail handleFaturaNaoEncontrada(FaturaNaoEncontradaException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("id", ex.getId().toString());
        return problem;
    }

    @ExceptionHandler(CarteiraNaoEncontradaException.class)
    public ProblemDetail handleCarteiraNaoEncontrada(CarteiraNaoEncontradaException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("id", ex.getId().toString());
        return problem;
    }

    @ExceptionHandler(GrupoNaoEncontradoException.class)
    public ProblemDetail handleGrupoNaoEncontrado(GrupoNaoEncontradoException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("id", ex.getId().toString());
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleEstadoInvalido(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Bad Request");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleArgumentoInvalido(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Bad Request");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(IncidenteNaoEncontradoException.class)
    public ProblemDetail handleIncidenteNaoEncontrado(IncidenteNaoEncontradoException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("codigo", ex.getCodigo());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenerico(Exception ex, HttpServletRequest request) {
        String operacao = request.getMethod() + " " + request.getRequestURI();
        String classeErro = ex.getClass().getSimpleName();
        String mensagem = ex.getMessage() != null
                ? ex.getMessage().substring(0, Math.min(ex.getMessage().length(), 500))
                : "(sem mensagem)";
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();
        if (stackTrace.length() > 4000) {
            stackTrace = stackTrace.substring(0, 4000);
        }

        String codigoErro = null;
        try {
            codigoErro = registrarErroUseCase.executar(
                    new RegistrarErroUseCase.Comando(operacao, classeErro, mensagem, stackTrace));
        } catch (Exception regEx) {
            LOG.error("Falha ao registrar incidente", regEx);
        }

        LOG.error("Erro nao tratado [{}]: {} {}", codigoErro != null ? codigoErro : "sem-codigo",
                operacao, ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal Server Error");
        if (codigoErro != null) {
            problem.setDetail("Erro inesperado. Informe ao suporte o codigo: " + codigoErro);
            problem.setProperty("codigoErro", codigoErro);
        } else {
            problem.setDetail("Ocorreu um erro interno. Tente novamente mais tarde.");
        }
        return problem;
    }
}
