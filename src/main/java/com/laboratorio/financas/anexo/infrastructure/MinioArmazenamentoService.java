package com.laboratorio.financas.anexo.infrastructure;

import com.laboratorio.financas.anexo.domain.ArmazenamentoService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Adaptador de armazenamento baseado em MinIO (API S3-compativel).
 *
 * <p>Encapsula as checked exceptions do SDK MinIO em {@link RuntimeException}
 * para nao vazar detalhes de infraestrutura para a camada de aplicacao.
 */
@Component
public class MinioArmazenamentoService implements ArmazenamentoService {

    private final MinioClient minioClient;
    private final String bucket;

    public MinioArmazenamentoService(MinioClient minioClient,
                                     @Value("${minio.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    @PostConstruct
    void garantirBucket() {
        try {
            boolean existe = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!existe) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao garantir o bucket MinIO: " + bucket, e);
        }
    }

    @Override
    public void upload(String chave, InputStream conteudo, long tamanho, String tipoConteudo) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(chave)
                    .stream(conteudo, tamanho, -1)
                    .contentType(tipoConteudo)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Falha ao enviar arquivo para o MinIO: " + chave, e);
        }
    }

    @Override
    public InputStream download(String chave) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(chave)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Falha ao baixar arquivo do MinIO: " + chave, e);
        }
    }

    @Override
    public void remover(String chave) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(chave)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Falha ao remover arquivo do MinIO: " + chave, e);
        }
    }

    @Override
    public String urlTemporaria(String chave, int expiracaoMinutos) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(chave)
                    .expiry(expiracaoMinutos, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar URL temporaria do MinIO: " + chave, e);
        }
    }
}
