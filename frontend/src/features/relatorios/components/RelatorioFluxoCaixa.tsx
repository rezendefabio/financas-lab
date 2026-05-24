import { Document, Page, Text, View, StyleSheet, PDFDownloadLink } from '@react-pdf/renderer'
import { formatBRL, formatDate } from '@/shared/lib/formatters'
import type { FluxoCaixa } from '@/features/dashboard'

// Estilos -- nunca usar CSS classes, apenas StyleSheet.create()
const styles = StyleSheet.create({
  page: { padding: 40, fontFamily: 'Helvetica', fontSize: 10 },
  header: { marginBottom: 24 },
  titulo: { fontSize: 18, fontWeight: 'bold', marginBottom: 4 },
  subtitulo: { fontSize: 11, color: '#6b7280' },
  cards: { marginTop: 12, flexDirection: 'column' },
  card: {
    borderWidth: 0.5,
    borderColor: '#e5e7eb',
    borderRadius: 4,
    padding: 16,
    marginBottom: 12,
  },
  cardLabel: { fontSize: 11, color: '#6b7280', marginBottom: 6 },
  cardValor: { fontSize: 22, fontWeight: 'bold' },
  cardValorReceita: { color: '#16a34a' },
  cardValorDespesa: { color: '#dc2626' },
  cardValorPositivo: { color: '#16a34a' },
  cardValorNegativo: { color: '#dc2626' },
  rodape: {
    marginTop: 24,
    borderTopWidth: 0.5,
    borderColor: '#e5e7eb',
    paddingTop: 8,
  },
  rodapeText: { fontSize: 9, color: '#9ca3af' },
})

const NOMES_MES_LONGO = [
  'Janeiro',
  'Fevereiro',
  'Marco',
  'Abril',
  'Maio',
  'Junho',
  'Julho',
  'Agosto',
  'Setembro',
  'Outubro',
  'Novembro',
  'Dezembro',
]

function formatMesAno(ano: number, mes: number): string {
  if (mes < 1 || mes > 12) return `${mes}/${ano}`
  return `${NOMES_MES_LONGO[mes - 1]} ${ano}`
}

// Documento interno (sem estado React -- apenas dados)
function RelatorioFluxoCaixaDocumento({ data }: { data: FluxoCaixa }) {
  const periodo = formatMesAno(data.ano, data.mes)
  const saldoNegativo = data.saldo < 0

  return (
    <Document>
      <Page size="A4" style={styles.page}>
        {/* Cabecalho */}
        <View style={styles.header}>
          <Text style={styles.titulo}>Fluxo de Caixa</Text>
          <Text style={styles.subtitulo}>{periodo}</Text>
        </View>

        {/* Cards verticais */}
        <View style={styles.cards}>
          <View style={styles.card}>
            <Text style={styles.cardLabel}>Total Receitas</Text>
            <Text style={[styles.cardValor, styles.cardValorReceita]}>
              {formatBRL(data.totalReceitas)}
            </Text>
          </View>

          <View style={styles.card}>
            <Text style={styles.cardLabel}>Total Despesas</Text>
            <Text style={[styles.cardValor, styles.cardValorDespesa]}>
              {formatBRL(data.totalDespesas)}
            </Text>
          </View>

          <View style={styles.card}>
            <Text style={styles.cardLabel}>Saldo do Mes</Text>
            <Text
              style={[
                styles.cardValor,
                saldoNegativo ? styles.cardValorNegativo : styles.cardValorPositivo,
              ]}
            >
              {formatBRL(data.saldo)}
            </Text>
          </View>
        </View>

        {/* Rodape */}
        <View style={styles.rodape}>
          <Text style={styles.rodapeText}>
            Gerado em {formatDate(new Date().toISOString())}
          </Text>
        </View>
      </Page>
    </Document>
  )
}

// Componente publico: documento (para uso direto em <PDFViewer>)
export function RelatorioFluxoCaixa({ data }: { data: FluxoCaixa }) {
  return <RelatorioFluxoCaixaDocumento data={data} />
}

// Componente publico: botao de download
export function PDFDownloadLinkFluxoCaixa({ data }: { data: FluxoCaixa }) {
  const mesStr = String(data.mes).padStart(2, '0')
  return (
    <PDFDownloadLink
      document={<RelatorioFluxoCaixaDocumento data={data} />}
      fileName={`relatorio-fluxo-caixa-${data.ano}-${mesStr}.pdf`}
    >
      {({ loading }) => (loading ? 'Gerando PDF...' : 'Baixar PDF')}
    </PDFDownloadLink>
  )
}
