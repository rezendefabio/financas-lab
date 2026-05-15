export interface Tag {
  id: string;
  userId: string;
  nome: string;
  cor?: string;
  criadoEm: string;
}

export interface CriarTagRequest {
  nome: string;
  cor?: string;
}
