package com.example.drones.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrdersRepository extends JpaRepository<OrdersEntity, UUID>, JpaSpecificationExecutor<OrdersEntity> {
    List<OrdersEntity> findAllByStatus(OrderStatus status);

    @Query("""
                        SELECT o
                        FROM OrdersEntity o
                        LEFT JOIN FETCH o.user u
                        LEFT JOIN FETCH u.portfolio p
                        WHERE o.id = :orderId
            """)
    Optional<OrdersEntity> findByIdWithUser(UUID orderId);
}