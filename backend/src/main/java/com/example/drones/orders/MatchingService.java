package com.example.drones.orders;

import com.example.drones.services.OperatorServicesEntity;
import com.example.drones.services.OperatorServicesRepository;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
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
    private final EmailService emailService;
    private final OperatorServicesRepository operatorServicesRepository;

    @Async
    @Transactional
    public void matchOperatorsToOrder(OrdersEntity order) {
        try {
            Pair<Double, Double> orderCoords = parseCoordinates(order.getCoordinates());
            double orderLat = orderCoords.getFirst();
            double orderLon = orderCoords.getSecond();
            List<UserEntity> matchingOperators = userRepository.findMatchingOperators(
                    order.getService().getName(),
                    orderLat,
                    orderLon,
                    order.getUser().getId()
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

            matchingOperators.forEach(operator ->
                    emailService.sendNewOrderNotification(operator, order)
            );

        } catch (Exception e) {
            log.error("Error during matching operators for order {}", order.getId(), e);
        }
    }

    @Async
    @Transactional
    public void matchOrdersToNewOperator(UserEntity operator) {
        Pair<Double, Double> operatorCoords = parseCoordinates(operator.getCoordinates());
        double operatorLat = operatorCoords.getFirst();
        double operatorLon = operatorCoords.getSecond();
        List<String> operatorServices = operatorServicesRepository.findAllByOperatorId(operator.getId()).stream()
                .map(OperatorServicesEntity::getServiceName)
                .toList();

        List<OrdersEntity> matchingOrders = userRepository.findMatchingOrdersForOperator(
                operator.getId(),
                operatorLat,
                operatorLon,
                operator.getRadius(),
                operatorServices
        );

        log.info("Found {} matching orders for operator {}", matchingOrders.size(), operator.getId());

        List<NewMatchedOrderEntity> matches = matchingOrders.stream()
                .map(order -> NewMatchedOrderEntity.builder()
                        .order(order)
                        .operator(operator)
                        .clientStatus(MatchedOrderStatus.PENDING)
                        .operatorStatus(MatchedOrderStatus.PENDING)
                        .build())
                .toList();

        newMatchedOrdersRepository.saveAll(matches);
    }

    private Pair<Double, Double> parseCoordinates(String coordinates) {
        String[] parts = coordinates.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid coordinates format");
        }
        double lat = Double.parseDouble(parts[0].trim());
        double lon = Double.parseDouble(parts[1].trim());
        return Pair.of(lat, lon);
    }
}