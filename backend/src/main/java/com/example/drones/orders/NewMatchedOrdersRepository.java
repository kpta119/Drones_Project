package com.example.drones.orders;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NewMatchedOrdersRepository extends JpaRepository<NewMatchedOrderEntity, Integer> {

    Optional<NewMatchedOrderEntity> findByOrderIdAndOperatorId(UUID orderId, UUID userId);
}
