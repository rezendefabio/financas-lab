CREATE TABLE instituicao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(100) NOT NULL,
    codigo_banco VARCHAR(10),
    tipo VARCHAR(30) NOT NULL,
    logo_url VARCHAR(255),
    ativa BOOLEAN NOT NULL DEFAULT true,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Seed: 15 principais instituicoes do Brasil
INSERT INTO instituicao (nome, codigo_banco, tipo) VALUES
    ('Banco do Brasil', '001', 'BANCO_TRADICIONAL'),
    ('Caixa Economica Federal', '104', 'BANCO_TRADICIONAL'),
    ('Bradesco', '237', 'BANCO_TRADICIONAL'),
    ('Itau Unibanco', '341', 'BANCO_TRADICIONAL'),
    ('Santander', '033', 'BANCO_TRADICIONAL'),
    ('Nubank', '260', 'BANCO_DIGITAL'),
    ('Inter', '077', 'BANCO_DIGITAL'),
    ('C6 Bank', '336', 'BANCO_DIGITAL'),
    ('Next', '237', 'BANCO_DIGITAL'),
    ('PicPay', '380', 'CARTEIRA_DIGITAL'),
    ('Mercado Pago', '323', 'CARTEIRA_DIGITAL'),
    ('XP Investimentos', '102', 'CORRETORA'),
    ('BTG Pactual', '208', 'BANCO_TRADICIONAL'),
    ('Sicoob', '756', 'BANCO_TRADICIONAL'),
    ('Banco Original', '212', 'BANCO_DIGITAL');
