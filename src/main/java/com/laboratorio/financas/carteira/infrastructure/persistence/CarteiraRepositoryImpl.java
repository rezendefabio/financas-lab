package com.laboratorio.financas.carteira.infrastructure.persistence;

import com.laboratorio.financas.carteira.domain.Carteira;
import com.laboratorio.financas.carteira.domain.CarteiraRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CarteiraRepositoryImpl implements CarteiraRepository {

    private final CarteiraJpaRepository jpaRepository;
    private final CarteiraMapper mapper;

    public CarteiraRepositoryImpl(CarteiraJpaRepository jpaRepository, CarteiraMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Carteira salvar(Carteira carteira) {
        CarteiraEntity entity = mapper.toEntity(carteira);
        CarteiraEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Carteira> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Carteira> listarPorUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Carteira atualizar(Carteira carteira) {
        CarteiraEntity entity = mapper.toEntity(carteira);
        CarteiraEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void deletar(UUID id) {
        jpaRepository.deleteById(id);
    }
}
