package com.example.drones.orders;

import com.example.drones.orders.dto.OrderResponseWithOperatorId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
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
            SELECT new com.example.drones.orders.dto.OrderResponseWithOperatorId(
                    o.id,
                    o.userId,
                    o.title,
                    o.description,
                    o.service.name,
                    o.parameters,
                    o.coordinates,
                    o.fromDate,
                    o.toDate,
                    o.status,
                    o.createdAt,
                    CASE
                        WHEN o.status IN :visibleStatuses THEN nmo.operator.id
                        ELSE null
                    END
                    )
                    FROM OrdersEntity o
                    LEFT JOIN NewMatchedOrderEntity nmo ON nmo.order = o
                        AND nmo.clientStatus = 'ACCEPTED'
                        AND nmo.operatorStatus = 'ACCEPTED'
                    WHERE o.user.id = :userId
                    AND (:#{#status == null} = true OR o.status = :status)
                    ORDER BY o.createdAt desc
            
            """)
    Page<OrderResponseWithOperatorId> findAllByUserIdAndOrderStatus(UUID userId, OrderStatus status, List<OrderStatus> visibleStatuses, Pageable pageable);

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