package com.example.drones.orders;

import com.example.drones.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NewMatchedOrdersRepository extends JpaRepository<NewMatchedOrderEntity, Integer> {

    Optional<NewMatchedOrderEntity> findByOrderIdAndOperatorId(UUID orderId, UUID userId);

    @Query("""
                        SELECT nmo.operator
                        FROM NewMatchedOrderEntity nmo
                        LEFT JOIN FETCH nmo.operator.portfolio p
                        WHERE nmo.order.id = :orderId
                        AND nmo.operatorStatus = 'ACCEPTED'
            
            """)
    List<UserEntity> findInterestedOperatorByOrderId(UUID orderId);
}
