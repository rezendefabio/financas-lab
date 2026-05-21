-- V26: subcategorias de sistema para enriquecer a experiencia do usuario
-- UUIDs fixos (prefixo c1) garantem idempotencia via ON CONFLICT
-- categoria_pai_id resolvido por nome para suportar DBs cujos UUIDs do V10
-- foram substituidos por dedupe do V20 ou insercao manual anterior

INSERT INTO categoria (id, nome, tipo, categoria_pai_id, system, criado_em, atualizado_em)
VALUES

-- Alimentacao
('c1000000-0000-0000-0000-000000000001', 'Supermercado',     'DESPESA', (SELECT id FROM categoria WHERE nome = 'Alimentacao' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000002', 'Restaurante',      'DESPESA', (SELECT id FROM categoria WHERE nome = 'Alimentacao' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000003', 'Lanchonete',       'DESPESA', (SELECT id FROM categoria WHERE nome = 'Alimentacao' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000004', 'Delivery',         'DESPESA', (SELECT id FROM categoria WHERE nome = 'Alimentacao' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000005', 'Padaria',          'DESPESA', (SELECT id FROM categoria WHERE nome = 'Alimentacao' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),

-- Transporte
('c1000000-0000-0000-0000-000000000011', 'Combustivel',       'DESPESA', (SELECT id FROM categoria WHERE nome = 'Transporte' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000012', 'Transporte publico','DESPESA', (SELECT id FROM categoria WHERE nome = 'Transporte' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000013', 'Taxi/Uber',         'DESPESA', (SELECT id FROM categoria WHERE nome = 'Transporte' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000014', 'Estacionamento',    'DESPESA', (SELECT id FROM categoria WHERE nome = 'Transporte' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000015', 'Manutencao veicular','DESPESA', (SELECT id FROM categoria WHERE nome = 'Transporte' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),

-- Moradia
('c1000000-0000-0000-0000-000000000021', 'Aluguel',             'DESPESA', (SELECT id FROM categoria WHERE nome = 'Moradia' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000022', 'Energia eletrica',    'DESPESA', (SELECT id FROM categoria WHERE nome = 'Moradia' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000023', 'Agua e esgoto',       'DESPESA', (SELECT id FROM categoria WHERE nome = 'Moradia' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000024', 'Internet',            'DESPESA', (SELECT id FROM categoria WHERE nome = 'Moradia' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000025', 'Condominio',          'DESPESA', (SELECT id FROM categoria WHERE nome = 'Moradia' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000026', 'IPTU',                'DESPESA', (SELECT id FROM categoria WHERE nome = 'Moradia' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000027', 'Reforma e manutencao','DESPESA', (SELECT id FROM categoria WHERE nome = 'Moradia' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),

-- Saude
('c1000000-0000-0000-0000-000000000031', 'Plano de saude',  'DESPESA', (SELECT id FROM categoria WHERE nome = 'Saude' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000032', 'Consulta medica', 'DESPESA', (SELECT id FROM categoria WHERE nome = 'Saude' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000033', 'Farmacia',        'DESPESA', (SELECT id FROM categoria WHERE nome = 'Saude' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000034', 'Exames',          'DESPESA', (SELECT id FROM categoria WHERE nome = 'Saude' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000035', 'Academia',        'DESPESA', (SELECT id FROM categoria WHERE nome = 'Saude' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),

-- Educacao
('c1000000-0000-0000-0000-000000000041', 'Mensalidade escola','DESPESA', (SELECT id FROM categoria WHERE nome = 'Educacao' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000042', 'Faculdade/Pos',     'DESPESA', (SELECT id FROM categoria WHERE nome = 'Educacao' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000043', 'Cursos online',     'DESPESA', (SELECT id FROM categoria WHERE nome = 'Educacao' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000044', 'Livros',            'DESPESA', (SELECT id FROM categoria WHERE nome = 'Educacao' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000045', 'Material escolar',  'DESPESA', (SELECT id FROM categoria WHERE nome = 'Educacao' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),

-- Lazer
('c1000000-0000-0000-0000-000000000051', 'Cinema/Teatro',  'DESPESA', (SELECT id FROM categoria WHERE nome = 'Lazer' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000052', 'Viagens',        'DESPESA', (SELECT id FROM categoria WHERE nome = 'Lazer' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000053', 'Jogos',          'DESPESA', (SELECT id FROM categoria WHERE nome = 'Lazer' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000054', 'Bares e festas', 'DESPESA', (SELECT id FROM categoria WHERE nome = 'Lazer' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000055', 'Esportes',       'DESPESA', (SELECT id FROM categoria WHERE nome = 'Lazer' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),

-- Vestuario
('c1000000-0000-0000-0000-000000000061', 'Roupas',     'DESPESA', (SELECT id FROM categoria WHERE nome = 'Vestuario' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000062', 'Calcados',   'DESPESA', (SELECT id FROM categoria WHERE nome = 'Vestuario' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000063', 'Acessorios', 'DESPESA', (SELECT id FROM categoria WHERE nome = 'Vestuario' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),

-- Assinaturas
('c1000000-0000-0000-0000-000000000071', 'Streaming video',  'DESPESA', (SELECT id FROM categoria WHERE nome = 'Assinaturas' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000072', 'Streaming musica', 'DESPESA', (SELECT id FROM categoria WHERE nome = 'Assinaturas' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000073', 'Software/Apps',    'DESPESA', (SELECT id FROM categoria WHERE nome = 'Assinaturas' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000074', 'Clube/Associacao', 'DESPESA', (SELECT id FROM categoria WHERE nome = 'Assinaturas' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),

-- Salario
('c1000000-0000-0000-0000-000000000081', 'Salario CLT',         'RECEITA', (SELECT id FROM categoria WHERE nome = 'Salario' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000082', 'Bonus',               'RECEITA', (SELECT id FROM categoria WHERE nome = 'Salario' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000083', 'Participacao lucros', 'RECEITA', (SELECT id FROM categoria WHERE nome = 'Salario' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000084', '13 salario',          'RECEITA', (SELECT id FROM categoria WHERE nome = 'Salario' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000085', 'Ferias',              'RECEITA', (SELECT id FROM categoria WHERE nome = 'Salario' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),

-- Freelance
('c1000000-0000-0000-0000-000000000091', 'Projeto pontual',    'RECEITA', (SELECT id FROM categoria WHERE nome = 'Freelance' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000092', 'Consultoria',        'RECEITA', (SELECT id FROM categoria WHERE nome = 'Freelance' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-000000000093', 'Aulas particulares', 'RECEITA', (SELECT id FROM categoria WHERE nome = 'Freelance' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),

-- Renda Extra
('c1000000-0000-0000-0000-0000000000a1', 'Aluguel de imovel', 'RECEITA', (SELECT id FROM categoria WHERE nome = 'Renda Extra' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-0000000000a2', 'Dividendos',        'RECEITA', (SELECT id FROM categoria WHERE nome = 'Renda Extra' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-0000000000a3', 'Venda de bens',     'RECEITA', (SELECT id FROM categoria WHERE nome = 'Renda Extra' AND system = true AND categoria_pai_id IS NULL), true, now(), now()),
('c1000000-0000-0000-0000-0000000000a4', 'Cashback',          'RECEITA', (SELECT id FROM categoria WHERE nome = 'Renda Extra' AND system = true AND categoria_pai_id IS NULL), true, now(), now())

ON CONFLICT (id) DO NOTHING;
