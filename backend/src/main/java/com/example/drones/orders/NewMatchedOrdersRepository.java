package com.example.drones.orders;

import com.example.drones.operators.dto.MatchingOperatorDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NewMatchedOrdersRepository extends JpaRepository<NewMatchedOrderEntity, Integer> {

    Optional<NewMatchedOrderEntity> findByOrderIdAndOperatorId(UUID orderId, UUID userId);

    boolean existsByOrderIdAndClientStatus(UUID orderId, MatchedOrderStatus clientStatus);

    @Query("""

            SELECT new com.example.drones.operators.dto.MatchingOperatorDto(
                nmo.operator.id,
                nmo.operator.displayName,
                nmo.operator.name,
                nmo.operator.surname,
                nmo.operator.certificates,
                AVG(r.stars)
                )
            FROM NewMatchedOrderEntity nmo
            LEFT JOIN nmo.operator.portfolio p
            LEFT JOIN ReviewEntity r on r.target.id = nmo.operator.id
            WHERE nmo.order.id = :orderId
            AND nmo.operatorStatus = 'ACCEPTED'
            AND nmo.clientStatus = 'PENDING'
            GROUP BY nmo.operator.id
            
            """)
    List<MatchingOperatorDto> findInterestedOperatorByOrderId(UUID orderId);
}
