package com.laboratorio.financas.transacao.infrastructure.persistence;

import com.laboratorio.financas.transacao.domain.OrdenacaoTransacao;

/**
 * Traduz um campo de ordenacao de dominio ({@link OrdenacaoTransacao}) no
 * caminho de propriedade da entidade JPA {@link TransacaoEntity}.
 *
 * <p>Esta traducao vive na camada de infraestrutura porque somente ela conhece
 * o modelo de persistencia -- em particular, que {@code valor} e um
 * {@code @Embedded MoneyEmbeddable} e portanto exige o caminho
 * {@code valor.valor}. As camadas {@code domain} e {@code interfaces}
 * permanecem ignorantes desse detalhe.
 */
final class OrdenacaoTransacaoJpaPath {

    private OrdenacaoTransacaoJpaPath() {
    }

    /**
     * Retorna o caminho da propriedade JPA correspondente ao campo de dominio.
     *
     * @param ordenacao campo de ordenacao de dominio
     * @return caminho da propriedade na entidade JPA
     */
    static String resolver(OrdenacaoTransacao ordenacao) {
        return switch (ordenacao) {
            // valor e um @Embedded MoneyEmbeddable: ordena pela propriedade interna.
            case VALOR -> "valor.valor";
            case DATA -> "data";
            case DESCRICAO -> "descricao";
            case TIPO -> "tipo";
            case STATUS -> "status";
        };
    }
}
