package com.example.drones.orders;

import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

    private final UserRepository userRepository;
    private final NewMatchedOrdersRepository newMatchedOrdersRepository;

    @Async
    @Transactional
    public void matchOperatorsToOrder(OrdersEntity order) {
        try {
            String[] coords = order.getCoordinates().split(",");
            if (coords.length != 2) {
                log.warn("Invalid coordinates for order {}", order.getId());
                return;
            }
            double orderLat = Double.parseDouble(coords[0].trim());
            double orderLon = Double.parseDouble(coords[1].trim());

            List<UserEntity> matchingOperators = userRepository.findMatchingOperators(
                    order.getService().getName(),
                    orderLat,
                    orderLon
            );

            log.info("Found {} matching operators for order {}", matchingOperators.size(), order.getId());

            List<NewMatchedOrderEntity> matches = matchingOperators.stream()
                    .map(operator -> NewMatchedOrderEntity.builder()
                            .order(order)
                            .operator(operator)
                            .clientStatus(MatchedOrderStatus.PENDING)
                            .operatorStatus(MatchedOrderStatus.PENDING)
                            .build())
                    .toList();

            newMatchedOrdersRepository.saveAll(matches);

            // Tutaj można dodać wysyłanie powiadomień do operatorów

        } catch (Exception e) {
            log.error("Error during matching operators for order {}", order.getId(), e);
        }
    }
}