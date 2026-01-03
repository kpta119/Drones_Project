package com.example.drones.admin;

import com.example.drones.admin.dto.OrderDto;
import com.example.drones.admin.dto.UserDto;
import com.example.drones.orders.MatchedOrderStatus;
import com.example.drones.orders.OrderStatus;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface AdminRepository extends JpaRepository<UserEntity, UUID> {

    @Query("""
                SELECT new com.example.drones.admin.dto.UserDto(
                    CAST(u.id AS string),
                    u.displayName,
                    u.role,
                    u.name,
                    u.surname,
                    u.email,
                    u.phoneNumber
                )
                FROM UserEntity u
                WHERE (:query IS NULL OR
                       u.displayName ILIKE %:query% OR
                       u.email ILIKE %:query%)
                  AND (CAST(:role AS string) IS NULL OR u.role = :role)
            """)
    Page<UserDto> findAllByQueryAndRole(@Param("query") String query, @Param("role") UserRole role, Pageable pageable);

    @Query(""" 
            SELECT new com.example.drones.admin.dto.OrderDto(
                o.id,
                o.title,
                o.description,
                o.service.name,
                o.coordinates,
                o.fromDate,
                o.toDate,
                o.status,
                o.createdAt,
                o.user.id,
                CASE
                    WHEN o.status IN :visibleStatuses THEN nmo.operator.id
                    ELSE null
                END
            )
            FROM OrdersEntity o
            LEFT JOIN NewMatchedOrderEntity nmo ON nmo.order = o
                AND nmo.clientStatus = :acceptedStatus
                AND nmo.operatorStatus = :acceptedStatus
            """)
    Page<OrderDto> findAllOrders(
            @Param("visibleStatuses") List<OrderStatus> visibleStatuses,
            @Param("acceptedStatus") MatchedOrderStatus acceptedStatus,
            Pageable pageable
    );

    @Query(value = """
    WITH top_operator AS (
        SELECT nmo.operator_id, COUNT(*) as completed_count
        FROM new_matched_orders nmo
        JOIN orders o ON nmo.order_id = o.id
        WHERE nmo.operator_status = 'ACCEPTED'::matched_order_status
        AND nmo.client_status = 'ACCEPTED'::matched_order_status
        AND o.status = 'COMPLETED'::order_status
        GROUP BY nmo.operator_id
        ORDER BY COUNT(*) DESC
        LIMIT 1
    )
    SELECT
        (SELECT COUNT(*) FROM users WHERE role = 'CLIENT'::user_role) as clientsCount,
        (SELECT COUNT(*) FROM users WHERE role = 'OPERATOR'::user_role) as operatorsCount,
        (SELECT COUNT(*) FROM orders WHERE status IN ('OPEN'::order_status, 'AWAITING_OPERATOR'::order_status, 'IN_PROGRESS'::order_status)) as activeOrders,
        (SELECT COUNT(*) FROM orders WHERE status = 'COMPLETED'::order_status) as completedOrders,
        (SELECT COUNT(DISTINCT nmo.operator_id)
         FROM new_matched_orders nmo
         JOIN orders o ON nmo.order_id = o.id
         WHERE nmo.operator_status = 'ACCEPTED'::matched_order_status
         AND nmo.client_status = 'ACCEPTED'::matched_order_status
         AND o.status = 'IN_PROGRESS'::order_status) as busyOperators,
        (SELECT operator_id FROM top_operator) as topOperatorId,
        COALESCE((SELECT completed_count FROM top_operator), 0) as topOperatorCompletedOrders,
        (SELECT COUNT(*) FROM reviews) as totalReviews
    """, nativeQuery = true)
    SystemStatsProjection getSystemStatistics();
}
