package com.example.drones.orders;

import com.example.drones.common.config.auth.JwtService;
import com.example.drones.orders.dto.OrderRequest;
import com.example.drones.orders.dto.OrderResponse;
import com.example.drones.orders.dto.OrderUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrdersController {

    private final OrdersService ordersService;
    private final JwtService jwtService;

    @PostMapping("/createOrder")
    @PreAuthorize("hasAnyRole('OPERATOR', 'CLIENT')")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid OrderRequest request) {
        UUID userId = jwtService.extractUserId();
        OrderResponse response = ordersService.createOrder(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/editOrder/{orderId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'CLIENT')")
    public ResponseEntity<OrderResponse> editOrder(
            @PathVariable UUID orderId,
            @RequestBody OrderUpdateRequest request
    ) {
        UUID userId = jwtService.extractUserId();
        OrderResponse response = ordersService.editOrder(orderId, request, userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/acceptOrder/{orderId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'CLIENT')")
    public ResponseEntity<OrderResponse> acceptOrder(
            @PathVariable UUID orderId,
            @RequestParam(required = false) UUID operatorId
    ) {
        UUID currentUserId = jwtService.extractUserId();
        OrderResponse response = ordersService.acceptOrder(orderId, operatorId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/rejectOrder/{orderId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'CLIENT')")
    public ResponseEntity<Void> rejectOrder(
            @PathVariable UUID orderId,
            @RequestParam(required = false) UUID operatorId
    ) {
        UUID currentUserId = jwtService.extractUserId();
        ordersService.rejectOrder(orderId, operatorId, currentUserId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/cancelOrder/{orderId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'CLIENT')")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID orderId) {
        UUID currentUserId = jwtService.extractUserId();
        OrderResponse response = ordersService.cancelOrder(orderId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getOrders/{status}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'CLIENT')")
    public ResponseEntity<List<OrderResponse>> getOrders(@PathVariable String status) {
        List<OrderResponse> responses = ordersService.getOrdersByStatus(status);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/getMyOrders")
    @PreAuthorize("hasAnyRole('OPERATOR', 'CLIENT')")
    public ResponseEntity<List<OrderResponse>> getMyOrders() {
        UUID userId = jwtService.extractUserId();
        List<OrderResponse> response = ordersService.getMyOrders(userId);
        return ResponseEntity.ok(response);
    }
}