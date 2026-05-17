import { Document, Page, Text, View, StyleSheet, PDFDownloadLink } from '@react-pdf/renderer'
import { formatBRL, formatDate } from '@/shared/lib/formatters'
import type { GastosPorCategoria } from '../types/relatorio'

// Estilos -- nunca usar CSS classes, apenas StyleSheet.create()
const styles = StyleSheet.create({
  page: { padding: 40, fontFamily: 'Helvetica', fontSize: 10 },
  header: { marginBottom: 20 },
  titulo: { fontSize: 16, fontWeight: 'bold', marginBottom: 4 },
  subtitulo: { fontSize: 10, color: '#6b7280' },
  tabela: { marginTop: 12 },
  thead: {
    flexDirection: 'row',
    borderBottomWidth: 1,
    borderColor: '#e5e7eb',
    paddingBottom: 4,
    marginBottom: 4,
  },
  theadCell: { flex: 1, fontWeight: 'bold', color: '#374151' },
  theadCellRight: { flex: 1, fontWeight: 'bold', color: '#374151', textAlign: 'right' },
  row: {
    flexDirection: 'row',
    paddingVertical: 3,
    borderBottomWidth: 0.5,
    borderColor: '#f3f4f6',
  },
  cell: { flex: 1, color: '#111827' },
  cellRight: { flex: 1, color: '#111827', textAlign: 'right' },
  rodape: {
    marginTop: 16,
    borderTopWidth: 0.5,
    borderColor: '#e5e7eb',
    paddingTop: 8,
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  rodapeText: { fontSize: 9, color: '#9ca3af' },
  total: { fontSize: 9, fontWeight: 'bold', color: '#374151' },
})

// Documento interno (sem estado React -- apenas dados)
function RelatorioGastosPorCategoriaDocumento({
  data,
  periodo,
}: {
  data: GastosPorCategoria
  periodo: string
}) {
  return (
    <Document>
      <Page size="A4" style={styles.page}>
        {/* Cabecalho */}
        <View style={styles.header}>
          <Text style={styles.titulo}>Gastos por Categoria</Text>
          <Text style={styles.subtitulo}>Periodo: {periodo}</Text>
        </View>

        {/* Tabela */}
        <View style={styles.tabela}>
          <View style={styles.thead}>
            <Text style={styles.theadCell}>Categoria</Text>
            <Text style={styles.theadCellRight}>Total Gasto</Text>
          </View>
          {data.itensPorCategoria.map((item, index) => (
            <View key={item.categoriaId ?? `sem-categoria-${index}`} style={styles.row}>
              <Text style={styles.cell}>{item.nomeCategoria}</Text>
              <Text style={styles.cellRight}>{formatBRL(item.totalGasto.valor)}</Text>
            </View>
          ))}
        </View>

        {/* Rodape */}
        <View style={styles.rodape}>
          <Text style={styles.rodapeText}>
            Gerado em {formatDate(new Date().toISOString())}
          </Text>
          <Text style={styles.total}>Total geral: {formatBRL(data.totalGeral.valor)}</Text>
        </View>
      </Page>
    </Document>
  )
}

// Componente publico: botao de download
export function RelatorioGastosPorCategoria({
  data,
  periodo,
}: {
  data: GastosPorCategoria
  periodo: string
}) {
  return (
    <PDFDownloadLink
      document={<RelatorioGastosPorCategoriaDocumento data={data} periodo={periodo} />}
      fileName={`relatorio-gastos-por-categoria-${periodo}.pdf`}
    >
      {({ loading }) => (loading ? 'Gerando PDF...' : 'Baixar PDF')}
    </PDFDownloadLink>
  )
}
