package com.example.drones.reviews;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewsRepository extends JpaRepository<ReviewEntity, Integer> {
    boolean existsByOrderIdAndAuthorIdAndTargetId(UUID orderId, UUID authorId, UUID targetId);
}
