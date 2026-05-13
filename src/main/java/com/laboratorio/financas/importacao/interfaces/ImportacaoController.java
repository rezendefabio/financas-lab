package com.laboratorio.financas.importacao.interfaces;

import com.laboratorio.financas.importacao.application.ImportarTransacoesCsvUseCase;
import com.laboratorio.financas.importacao.interfaces.dto.ImportacaoResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/importacoes")
public class ImportacaoController {

    private final ImportarTransacoesCsvUseCase useCase;

    public ImportacaoController(ImportarTransacoesCsvUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportacaoResponse importarCsv(
            @RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        byte[] conteudo = arquivo.getBytes();
        ImportarTransacoesCsvUseCase.Resultado resultado = useCase.importar(conteudo);
        return ImportacaoResponse.fromResultado(resultado);
    }
}
