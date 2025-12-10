package com.example.drones.photos;

import com.example.drones.operators.PortfolioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotosRepository extends JpaRepository<PhotoEntity, Integer> {
    List<PhotoEntity> findAllByPortfolio(PortfolioEntity portfolio);
}
