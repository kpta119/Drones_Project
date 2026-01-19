package com.example.drones.reviews;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewsRepository extends JpaRepository<ReviewEntity, Integer> {
    boolean existsByOrderIdAndAuthorIdAndTargetId(UUID orderId, UUID authorId, UUID targetId);

    @Query("SELECT r FROM ReviewEntity r JOIN FETCH r.author WHERE r.target.id = :userId")
    List<ReviewEntity> findAllByTargetId(@Param("userId") UUID userId);

    @Query("SELECT AVG(r.stars) FROM ReviewEntity r WHERE r.target.id = :userId")
    Double getAverageStars(UUID userId);

}
