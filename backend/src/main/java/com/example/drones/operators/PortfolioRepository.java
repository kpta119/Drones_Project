package com.example.drones.operators;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository extends JpaRepository<PortfolioEntity, Integer> {
    Optional<PortfolioEntity> findByOperatorId(UUID operatorId);
}
