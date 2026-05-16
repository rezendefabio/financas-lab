package com.laboratorio.financas.categoria.infrastructure.persistence;

import com.laboratorio.financas.categoria.domain.TipoCategoria;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoriaJpaRepository extends JpaRepository<CategoriaEntity, UUID> {

    List<CategoriaEntity> findByTipo(TipoCategoria tipo);

    List<CategoriaEntity> findByCategoriaPaiIdIsNull();

    List<CategoriaEntity> findByCategoriaPaiId(UUID categoriaPaiId);

    @Query("SELECT c FROM CategoriaEntity c WHERE c.system = true OR c.userId = :userId")
    List<CategoriaEntity> findVisiveisPara(@Param("userId") UUID userId);

    boolean existsByNomeAndUserId(String nome, UUID userId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CategoriaEntity c WHERE c.nome = :nome AND c.system = true")
    boolean existsByNomeAndSystemTrue(@Param("nome") String nome);
}
