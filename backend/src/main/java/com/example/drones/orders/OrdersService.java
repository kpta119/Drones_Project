package com.example.drones.orders;

import com.example.drones.auth.exceptions.InvalidCredentialsException;
import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.orders.dto.OrderRequest;
import com.example.drones.orders.dto.OrderResponse;
import com.example.drones.orders.dto.OrderUpdateRequest;
import com.example.drones.orders.exceptions.*;
import com.example.drones.services.ServicesEntity;
import com.example.drones.services.ServicesRepository;
import com.example.drones.services.exceptions.ServiceNotFoundException;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import com.example.drones.user.exceptions.NotOperatorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrdersService {

    private final OrdersRepository ordersRepository;
    private final UserRepository userRepository;
    private final ServicesRepository servicesRepository;
    private final OrdersMapper ordersMapper;
    private final NewMatchedOrdersRepository newMatchedOrdersRepository;
    private final Clock clock;
    private final MatchingService matchingService;

    @Transactional
    public OrderResponse createOrder(OrderRequest request, UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        ServicesEntity serviceEntity = servicesRepository.findById(request.getService())
                .orElseThrow(ServiceNotFoundException::new);

        OrdersEntity orderEntity = ordersMapper.toEntity(request, serviceEntity);

        orderEntity.setCreatedAt(LocalDateTime.now(clock));
        orderEntity.setUser(user);
        OrdersEntity savedOrder = ordersRepository.save(orderEntity);

        matchingService.matchOperatorsToOrder(savedOrder);

        return ordersMapper.toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse editOrder(UUID orderId, OrderUpdateRequest request, UUID userId) {
        OrdersEntity order = ordersRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (!order.getUser().getId().equals(userId)) {
            throw new InvalidCredentialsException();
        }

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new OrderIsNotEditableException();
        }

        if (request.getService() != null) {
            ServicesEntity newService = servicesRepository.findById(request.getService())
                    .orElseThrow(ServiceNotFoundException::new);
            order.setService(newService);
        }

        ordersMapper.updateEntityFromRequest(request, order);
        OrdersEntity updatedOrder = ordersRepository.save(order);

        return ordersMapper.toResponse(updatedOrder);
    }

    @Transactional
    public OrderResponse acceptOrder(UUID orderId, UUID operatorIdParam, UUID currentUserId) {
        UserEntity currentUser = userRepository.findById(currentUserId)
                .orElseThrow(UserNotFoundException::new);

        OrdersEntity foundOrder = ordersRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        NewMatchedOrderEntity match;
        if (operatorIdParam == null) {
            // Akceptuje operator
            if (currentUser.getRole() != UserRole.OPERATOR) {
                throw new NotOperatorException();
            }

            if (foundOrder.getUser().getId().equals(currentUserId)) {
                throw new CannotAcceptOwnOrderException();
            }

            match = newMatchedOrdersRepository.findByOrderIdAndOperatorId(orderId, currentUserId)
                    .orElseThrow(MatchedOrderNotFoundException::new);
            match.setOperatorStatus(MatchedOrderStatus.ACCEPTED);
            foundOrder.setStatus(OrderStatus.AWAITING_OPERATOR);
        } else {
            // Akceptuje zleceniodawca
            if (!foundOrder.getUser().getId().equals(currentUserId)) {
                throw new NotOwnerOfOrderException();
            }

            match = newMatchedOrdersRepository.findByOrderIdAndOperatorId(orderId, operatorIdParam)
                    .orElseThrow(MatchedOrderNotFoundException::new);
            match.setClientStatus(MatchedOrderStatus.ACCEPTED);
            if (match.getOperatorStatus() == MatchedOrderStatus.ACCEPTED) {
                foundOrder.setStatus(OrderStatus.IN_PROGRESS);
            }
        }

        newMatchedOrdersRepository.save(match);
        ordersRepository.save(foundOrder);
        return ordersMapper.toResponse(foundOrder);
    }
}