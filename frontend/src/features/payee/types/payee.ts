export interface Payee {
  id: string;
  userId: string;
  nome: string;
  categoriaPadraoId?: string;
  criadoEm: string;
  atualizadoEm: string;
}

export interface CriarPayeeRequest {
  nome: string;
  categoriaPadraoId?: string;
}
