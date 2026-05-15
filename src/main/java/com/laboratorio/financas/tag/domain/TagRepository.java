package com.laboratorio.financas.tag.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository {

    Optional<Tag> findById(UUID id);

    List<Tag> findByUserId(UUID userId);

    Optional<Tag> findByIdAndUserId(UUID id, UUID userId);

    Tag save(Tag tag);

    void deleteById(UUID id);
}
