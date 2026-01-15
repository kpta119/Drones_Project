package com.example.drones.orders;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrdersRepository extends JpaRepository<OrdersEntity, UUID>, JpaSpecificationExecutor<OrdersEntity> {

    @Query("""
                        SELECT o
                        FROM OrdersEntity o
                        LEFT JOIN FETCH o.user u
                        LEFT JOIN FETCH u.portfolio p
                        WHERE o.id = :orderId
            """)
    Optional<OrdersEntity> findByIdWithUser(UUID orderId);

    @Query("""
                    SELECT o
                    FROM OrdersEntity o
                    WHERE o.user.id = :userId
                    AND (:#{#status == null} = true OR o.status = :status)
                    ORDER BY o.createdAt desc
            
            """)
    Page<OrdersEntity> findAllByUserIdAndOrderStatus(UUID userId, OrderStatus status, Pageable pageable);

    @Query("""
            SELECT o
            FROM OrdersEntity o
            INNER JOIN o.matchedOrders nmo
            WHERE o.id = :orderId
            AND o.status = 'IN_PROGRESS'
            AND nmo.operator.id = :operatorId
            """)
    Optional<OrdersEntity> findInProgressOrderByOperatorId(UUID orderId, UUID operatorId);

    @Query("""
            SELECT o
            FROM OrdersEntity o
            INNER JOIN o.matchedOrders nmo
            WHERE o.status = 'IN_PROGRESS'
            AND nmo.operator.id = :operatorId
            AND nmo.clientStatus = 'ACCEPTED'
            AND nmo.operatorStatus = 'ACCEPTED'
            """)
    Page<OrdersEntity> findInProgressAndAcceptedOrdersByOperatorId(UUID operatorId, Pageable pageable);
}