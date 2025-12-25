package com.example.drones.photos;

import com.example.drones.operators.PortfolioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PhotosRepository extends JpaRepository<PhotoEntity, Integer> {
    List<PhotoEntity> findAllByPortfolio(PortfolioEntity portfolio);

    @Query("SELECT p FROM PhotoEntity p WHERE p.portfolio.operatorId = :userId AND p.id IN :ids")
    List<PhotoEntity> findMyPhotos(@Param("ids") List<Integer> photoIds, @Param("userId") UUID userId);
}
