ALTER TABLE categoria
    ADD COLUMN categoria_pai_id UUID,
    ADD CONSTRAINT fk_categoria_pai
        FOREIGN KEY (categoria_pai_id) REFERENCES categoria(id) ON DELETE SET NULL;
