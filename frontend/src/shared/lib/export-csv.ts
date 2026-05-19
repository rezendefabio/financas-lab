/**
 * Exportacao de dados para CSV no lado do cliente.
 *
 * Gera uma string CSV a partir de um conjunto de linhas e dispara o download
 * via elemento `<a download>`. O conteudo recebe um BOM UTF-8 para que o Excel
 * no Windows reconheca a codificacao corretamente.
 */

/** Definicao de coluna do CSV: `key` indexa o valor na linha, `label` vira o header. */
export interface CsvColumn {
  key: string
  label: string
}

/**
 * Escapa um valor de celula CSV.
 *
 * Valores com virgula, aspas duplas ou quebra de linha sao envolvidos em aspas
 * duplas; aspas internas sao duplicadas (regra RFC 4180).
 */
function escapeCsvValue(value: unknown): string {
  if (value === null || value === undefined) return ''
  const text = String(value)
  if (/[",\r\n]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

/**
 * Gera a string CSV (com header) a partir das linhas e colunas informadas.
 *
 * Exposto separadamente do download para permitir teste unitario do conteudo.
 */
export function buildCsv(
  rows: Record<string, unknown>[],
  columns: CsvColumn[],
): string {
  const header = columns.map((c) => escapeCsvValue(c.label)).join(',')
  const body = rows.map((row) =>
    columns.map((c) => escapeCsvValue(row[c.key])).join(','),
  )
  return [header, ...body].join('\r\n')
}

/**
 * Gera um CSV e dispara o download no browser.
 *
 * @param filename nome do arquivo (a extensao `.csv` e adicionada se ausente)
 * @param rows linhas de dados, cada uma um objeto indexado pela `key` da coluna
 * @param columns colunas do CSV, na ordem desejada
 */
export function exportToCsv(
  filename: string,
  rows: Record<string, unknown>[],
  columns: CsvColumn[],
): void {
  const csv = buildCsv(rows, columns)
  // BOM UTF-8: garante que o Excel no Windows leia acentos corretamente.
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename.endsWith('.csv') ? filename : `${filename}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
