package com.example.drones.orders;

import com.example.drones.auth.exceptions.InvalidCredentialsException;
import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.orders.dto.OrderRequest;
import com.example.drones.orders.dto.OrderResponse;
import com.example.drones.orders.dto.OrderUpdateRequest;
import com.example.drones.orders.exceptions.OrderIsNotEditableException;
import com.example.drones.orders.exceptions.OrderNotFoundException;
import com.example.drones.services.exceptions.ServiceNotFoundException;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import com.example.drones.services.ServicesEntity;
import com.example.drones.services.ServicesRepository;
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
    private final Clock clock;

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
}