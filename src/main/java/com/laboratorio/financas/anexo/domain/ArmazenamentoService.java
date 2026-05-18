package com.laboratorio.financas.anexo.domain;

import java.io.InputStream;

/**
 * Abstracao de armazenamento de objetos (arquivos binarios).
 *
 * <p>Implementada por adaptadores S3-compativeis (MinIO local, AWS S3 em
 * producao). Mantida no domain para nao acoplar a camada de aplicacao ao SDK.
 */
public interface ArmazenamentoService {

    void upload(String chave, InputStream conteudo, long tamanho, String tipoConteudo);

    InputStream download(String chave);

    void remover(String chave);

    String urlTemporaria(String chave, int expiracaoMinutos);
}
