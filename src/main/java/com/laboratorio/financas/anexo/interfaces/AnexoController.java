package com.laboratorio.financas.anexo.interfaces;

import com.laboratorio.financas.anexo.application.FazerUploadAnexoUseCase;
import com.laboratorio.financas.anexo.application.ListarAnexosPorEntidadeUseCase;
import com.laboratorio.financas.anexo.application.ObterUrlDownloadAnexoUseCase;
import com.laboratorio.financas.anexo.application.RemoverAnexoUseCase;
import com.laboratorio.financas.anexo.domain.Anexo;
import com.laboratorio.financas.anexo.interfaces.dto.AnexoResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/anexos")
public class AnexoController {

    private final FazerUploadAnexoUseCase fazerUploadAnexoUseCase;
    private final ObterUrlDownloadAnexoUseCase obterUrlDownloadAnexoUseCase;
    private final RemoverAnexoUseCase removerAnexoUseCase;
    private final ListarAnexosPorEntidadeUseCase listarAnexosPorEntidadeUseCase;

    public AnexoController(
            FazerUploadAnexoUseCase fazerUploadAnexoUseCase,
            ObterUrlDownloadAnexoUseCase obterUrlDownloadAnexoUseCase,
            RemoverAnexoUseCase removerAnexoUseCase,
            ListarAnexosPorEntidadeUseCase listarAnexosPorEntidadeUseCase
    ) {
        this.fazerUploadAnexoUseCase = fazerUploadAnexoUseCase;
        this.obterUrlDownloadAnexoUseCase = obterUrlDownloadAnexoUseCase;
        this.removerAnexoUseCase = removerAnexoUseCase;
        this.listarAnexosPorEntidadeUseCase = listarAnexosPorEntidadeUseCase;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AnexoResponse upload(
            @RequestParam MultipartFile arquivo,
            @RequestParam String entidadeTipo,
            @RequestParam UUID entidadeId
    ) throws IOException {
        FazerUploadAnexoUseCase.Comando comando = new FazerUploadAnexoUseCase.Comando(
                arquivo.getOriginalFilename(),
                arquivo.getContentType(),
                arquivo.getSize(),
                entidadeTipo,
                entidadeId,
                arquivo.getInputStream()
        );
        Anexo criado = fazerUploadAnexoUseCase.executar(comando);
        return AnexoResponse.de(criado);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Void> download(@PathVariable UUID id) {
        String url = obterUrlDownloadAnexoUseCase.executar(id);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }

    @GetMapping
    public List<AnexoResponse> listar(
            @RequestParam String entidadeTipo,
            @RequestParam UUID entidadeId
    ) {
        return listarAnexosPorEntidadeUseCase.executar(entidadeTipo, entidadeId).stream()
                .map(AnexoResponse::de)
                .toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable UUID id) {
        removerAnexoUseCase.executar(id);
    }
}
