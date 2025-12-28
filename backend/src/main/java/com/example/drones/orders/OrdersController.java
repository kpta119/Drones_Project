package com.example.drones.orders;

import com.example.drones.common.config.auth.JwtService;
import com.example.drones.orders.dto.OrderRequest;
import com.example.drones.orders.dto.OrderResponse;
import com.example.drones.orders.dto.OrderUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrdersController {

    private final OrdersService ordersService;
    private final JwtService jwtService;

    @PostMapping("/createOrder")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid OrderRequest request) {
        UUID userId = jwtService.extractUserId();
        OrderResponse response = ordersService.createOrder(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/editOrder/{orderId}")
    public ResponseEntity<OrderResponse> editOrder(
            @PathVariable UUID orderId,
            @RequestBody OrderUpdateRequest request
    ) {
        UUID userId = jwtService.extractUserId();
        OrderResponse response = ordersService.editOrder(orderId, request, userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/acceptOrder/{orderId}")
    public ResponseEntity<OrderResponse> acceptOrder(
            @PathVariable UUID orderId,
            @RequestParam(required = false) UUID operatorId
    ) {
        UUID currentUserId = jwtService.extractUserId();
        OrderResponse response = ordersService.acceptOrder(orderId, operatorId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/rejectOrder/{orderId}")
    public ResponseEntity<Void> rejectOrder(
            @PathVariable UUID orderId,
            @RequestParam(required = false) UUID operatorId
    ) {
        UUID currentUserId = jwtService.extractUserId();
        ordersService.rejectOrder(orderId, operatorId, currentUserId);
        return ResponseEntity.ok().build();
    }
}