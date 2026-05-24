import { Document, Page, Text, View, StyleSheet, PDFDownloadLink } from '@react-pdf/renderer'
import { formatBRL, formatDate } from '@/shared/lib/formatters'
import type { EvolucaoSaldo } from '../types/relatorio'

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
  cellRightNegativo: { flex: 1, color: '#dc2626', textAlign: 'right' },
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
  totalNegativo: { fontSize: 9, fontWeight: 'bold', color: '#dc2626' },
  rodapeColuna: { flexDirection: 'column', alignItems: 'flex-end' },
})

const NOMES_MES = [
  'Jan',
  'Fev',
  'Mar',
  'Abr',
  'Mai',
  'Jun',
  'Jul',
  'Ago',
  'Set',
  'Out',
  'Nov',
  'Dez',
]

// Converte "YYYY-MM" para "MMM/YY" (ex: "2026-05" -> "Mai/26")
function formatMesCurto(mes: string): string {
  const partes = mes.split('-')
  if (partes.length < 2) return mes
  const ano = partes[0]
  const mesNum = Number.parseInt(partes[1], 10)
  if (Number.isNaN(mesNum) || mesNum < 1 || mesNum > 12) return mes
  const nome = NOMES_MES[mesNum - 1]
  const anoCurto = ano.slice(-2)
  return `${nome}/${anoCurto}`
}

function calcularPeriodo(meses: EvolucaoSaldo['evolucaoPorMes']): string {
  if (meses.length === 0) return 'sem dados'
  if (meses.length === 1) return formatMesCurto(meses[0].mes)
  return `${formatMesCurto(meses[0].mes)} a ${formatMesCurto(meses[meses.length - 1].mes)}`
}

// Documento interno (sem estado React -- apenas dados)
function RelatorioEvolucaoSaldoDocumento({ data }: { data: EvolucaoSaldo }) {
  const meses = data.evolucaoPorMes
  const periodo = calcularPeriodo(meses)
  const totalReceitas = data.totalReceitas.valor
  const totalDespesas = data.totalDespesas.valor
  const saldoAcumulado = data.saldoLiquido.valor

  return (
    <Document>
      <Page size="A4" style={styles.page}>
        {/* Cabecalho */}
        <View style={styles.header}>
          <Text style={styles.titulo}>Evolucao de Saldo</Text>
          <Text style={styles.subtitulo}>Periodo: {periodo}</Text>
        </View>

        {/* Tabela */}
        <View style={styles.tabela}>
          <View style={styles.thead}>
            <Text style={styles.theadCell}>Mes</Text>
            <Text style={styles.theadCellRight}>Receitas</Text>
            <Text style={styles.theadCellRight}>Despesas</Text>
            <Text style={styles.theadCellRight}>Saldo</Text>
          </View>
          {meses.map((item) => (
            <View key={item.mes} style={styles.row}>
              <Text style={styles.cell}>{formatMesCurto(item.mes)}</Text>
              <Text style={styles.cellRight}>{formatBRL(item.totalReceitas.valor)}</Text>
              <Text style={styles.cellRight}>{formatBRL(item.totalDespesas.valor)}</Text>
              <Text
                style={
                  item.saldoLiquido.valor < 0 ? styles.cellRightNegativo : styles.cellRight
                }
              >
                {formatBRL(item.saldoLiquido.valor)}
              </Text>
            </View>
          ))}
        </View>

        {/* Rodape */}
        <View style={styles.rodape}>
          <Text style={styles.rodapeText}>
            Gerado em {formatDate(new Date().toISOString())}
          </Text>
          <View style={styles.rodapeColuna}>
            <Text style={styles.total}>Total receitas: {formatBRL(totalReceitas)}</Text>
            <Text style={styles.total}>Total despesas: {formatBRL(totalDespesas)}</Text>
            <Text style={saldoAcumulado < 0 ? styles.totalNegativo : styles.total}>
              Saldo acumulado: {formatBRL(saldoAcumulado)}
            </Text>
          </View>
        </View>
      </Page>
    </Document>
  )
}

// Componente publico: documento (para uso direto em <PDFViewer>)
export function RelatorioEvolucaoSaldo({ data }: { data: EvolucaoSaldo }) {
  return <RelatorioEvolucaoSaldoDocumento data={data} />
}

// Componente publico: botao de download
export function PDFDownloadLinkEvolucaoSaldo({ data }: { data: EvolucaoSaldo }) {
  const periodo = calcularPeriodo(data.evolucaoPorMes).replace(/[\s/]/g, '-')
  return (
    <PDFDownloadLink
      document={<RelatorioEvolucaoSaldoDocumento data={data} />}
      fileName={`relatorio-evolucao-saldo-${periodo}.pdf`}
    >
      {({ loading }) => (loading ? 'Gerando PDF...' : 'Baixar PDF')}
    </PDFDownloadLink>
  )
}
