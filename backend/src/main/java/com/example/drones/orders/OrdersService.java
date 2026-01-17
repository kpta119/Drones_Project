package com.example.drones.orders;

import com.example.drones.auth.exceptions.InvalidCredentialsException;
import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.orders.dto.OrderRequest;
import com.example.drones.orders.dto.OrderResponse;
import com.example.drones.orders.dto.OrderResponseWithOperatorId;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
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
    private final List<OrderStatus> statusesWithVisibleOperator = List.of(
            OrderStatus.IN_PROGRESS,
            OrderStatus.COMPLETED,
            OrderStatus.CANCELLED
    );

    @Transactional
    public OrderResponse createOrder(OrderRequest request, UUID userId) {
        LocalDateTime now = LocalDateTime.now(clock);

        if (request.getToDate().isBefore(now.toLocalDate().atStartOfDay())) {
            throw new IllegalOrderStateException("The end date cannot be earlier than today.");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        ServicesEntity serviceEntity = servicesRepository.findById(request.getService())
                .orElseThrow(ServiceNotFoundException::new);

        OrdersEntity orderEntity = ordersMapper.toEntity(request, serviceEntity);

        orderEntity.setCreatedAt(now);
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
            // Operator accepts
            if (currentUser.getRole() != UserRole.OPERATOR) {
                throw new NotOperatorException();
            }

            if (foundOrder.getUser().getId().equals(currentUserId)) {
                throw new CannotAcceptOwnOrderException();
            }

            match = newMatchedOrdersRepository.findByOrderIdAndOperatorId(orderId, currentUserId)
                    .orElseThrow(MatchedOrderNotFoundException::new);

            if (match.getOperatorStatus() == MatchedOrderStatus.ACCEPTED) {
                throw new OrderAlreadyAcceptedByYouException();
            }

            match.setOperatorStatus(MatchedOrderStatus.ACCEPTED);
            if (foundOrder.getStatus() == OrderStatus.OPEN) {
                foundOrder.setStatus(OrderStatus.AWAITING_OPERATOR);
            }
        } else {
            // Client accepts
            if (!foundOrder.getUser().getId().equals(currentUserId)) {
                throw new NotOwnerOfOrderException();
            }

            match = newMatchedOrdersRepository.findByOrderIdAndOperatorId(orderId, operatorIdParam)
                    .orElseThrow(MatchedOrderNotFoundException::new);

            if (match.getClientStatus() == MatchedOrderStatus.ACCEPTED) {
                throw new OrderAlreadyAcceptedByYouException();
            }

            boolean alreadyAcceptedSomeone = newMatchedOrdersRepository
                    .existsByOrderIdAndClientStatus(orderId, MatchedOrderStatus.ACCEPTED);
            if (alreadyAcceptedSomeone) {
                throw new OrderAlreadyHasAcceptedOperatorException();
            }


            match.setClientStatus(MatchedOrderStatus.ACCEPTED);
            if (match.getOperatorStatus() == MatchedOrderStatus.ACCEPTED) {
                foundOrder.setStatus(OrderStatus.IN_PROGRESS);
            }
        }

        newMatchedOrdersRepository.save(match);
        ordersRepository.save(foundOrder);
        return ordersMapper.toResponse(foundOrder);
    }

    @Transactional
    public void rejectOrder(UUID orderId, UUID operatorIdParam, UUID currentUserId) {
        UserEntity currentUser = userRepository.findById(currentUserId)
                .orElseThrow(UserNotFoundException::new);

        OrdersEntity foundOrder = ordersRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        NewMatchedOrderEntity match;

        if (operatorIdParam == null) {
            // Operator rejects an order
            if (currentUser.getRole() != UserRole.OPERATOR) {
                throw new NotOperatorException();
            }

            match = newMatchedOrdersRepository.findByOrderIdAndOperatorId(orderId, currentUserId)
                    .orElseThrow(MatchedOrderNotFoundException::new);

            match.setOperatorStatus(MatchedOrderStatus.REJECTED);
        } else {
            // Client rejects an operator
            if (!foundOrder.getUser().getId().equals(currentUserId)) {
                throw new NotOwnerOfOrderException();
            }

            match = newMatchedOrdersRepository.findByOrderIdAndOperatorId(orderId, operatorIdParam)
                    .orElseThrow(MatchedOrderNotFoundException::new);

            match.setClientStatus(MatchedOrderStatus.REJECTED);
        }

        newMatchedOrdersRepository.save(match);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UUID currentUserId) {
        OrdersEntity order = ordersRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (!order.getUser().getId().equals(currentUserId)) {
            throw new NotOwnerOfOrderException();
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalOrderStateException();
        }

        order.setStatus(OrderStatus.CANCELLED);
        OrdersEntity savedOrder = ordersRepository.save(order);
        return ordersMapper.toResponse(savedOrder);
    }

    public Page<OrderResponseWithOperatorId> getMyOrders(UUID userId, OrderStatus status, Pageable pageable) {
        userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        return ordersRepository.findAllByUserIdAndOrderStatus(userId, status, statusesWithVisibleOperator, pageable );
    }

    @Transactional
    public OrderResponse finishOrder(UUID orderId, UUID currentUserId) {
        OrdersEntity order = ordersRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (!order.getUser().getId().equals(currentUserId)) {
            throw new NotOwnerOfOrderException();
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalOrderStateException("The order has been already finished.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalOrderStateException("You cannot complete the order that has been cancelled.");
        }

        boolean hasAcceptedMatch = order.getMatchedOrders().stream()
                .anyMatch(match -> match.getOperatorStatus() == MatchedOrderStatus.ACCEPTED
                        && match.getClientStatus() == MatchedOrderStatus.ACCEPTED);

        if (!hasAcceptedMatch) {
            throw new IllegalOrderStateException("The order must be accepted by both client and operator before completion.");
        }

        order.setStatus(OrderStatus.COMPLETED);

        OrdersEntity savedOrder = ordersRepository.save(order);
        return ordersMapper.toResponse(savedOrder);
    }
}