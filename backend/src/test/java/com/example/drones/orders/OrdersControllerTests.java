package com.example.drones.orders;

import com.example.drones.auth.exceptions.InvalidCredentialsException;
import com.example.drones.common.config.auth.JwtService;
import com.example.drones.orders.OrderStatus;
import com.example.drones.orders.dto.OrderRequest;
import com.example.drones.orders.dto.OrderResponse;
import com.example.drones.orders.dto.OrderUpdateRequest;
import com.example.drones.orders.exceptions.OrderIsNotEditableException;
import com.example.drones.services.exceptions.ServiceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrdersControllerTests {

    @Mock
    private OrdersService ordersService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private OrdersController ordersController;

    private OrderRequest mockOrderRequest;
    private OrderUpdateRequest mockUpdateRequest;
    private OrderResponse mockOrderResponse;
    private UUID userId;
    private UUID orderId;

    @BeforeEach
    public void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        mockOrderRequest = OrderRequest.builder()
                .title("Inspekcja dachu")
                .description("Opis zlecenia")
                .service("Laser Scanning")
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        mockUpdateRequest = new OrderUpdateRequest();
        mockUpdateRequest.setDescription("Zaktualizowany opis");

        mockOrderResponse = OrderResponse.builder()
                .id(orderId)
                .title("Inspekcja dachu")
                .description("Opis zlecenia")
                .status(OrderStatus.OPEN)
                .build();
    }


    @Test
    public void givenValidRequest_whenCreateOrder_thenReturnsCreatedAndOrder() {
        when(jwtService.extractUserId()).thenReturn(userId);
        when(ordersService.createOrder(mockOrderRequest, userId)).thenReturn(mockOrderResponse);

        ResponseEntity<OrderResponse> response = ordersController.createOrder(mockOrderRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockOrderResponse.getTitle(), response.getBody().getTitle());

        verify(jwtService).extractUserId();
        verify(ordersService).createOrder(mockOrderRequest, userId);
    }

    @Test
    public void givenServiceException_whenCreateOrder_thenThrowsException() {
        when(jwtService.extractUserId()).thenReturn(userId);

        when(ordersService.createOrder(mockOrderRequest, userId))
                .thenThrow(new ServiceNotFoundException());

        RuntimeException exception = assertThrows(ServiceNotFoundException.class, () -> {
            ordersController.createOrder(mockOrderRequest);
        });

        assertEquals("Service not found", exception.getMessage());
        verify(ordersService).createOrder(mockOrderRequest, userId);
    }


    @Test
    public void givenValidUpdateRequest_whenEditOrder_thenReturnsOkAndUpdatedOrder() {
        OrderResponse updatedResponse = OrderResponse.builder()
                .id(orderId)
                .title("Inspekcja dachu")
                .description("Zaktualizowany opis")
                .status(OrderStatus.OPEN)
                .build();

        when(jwtService.extractUserId()).thenReturn(userId);
        when(ordersService.editOrder(orderId, mockUpdateRequest, userId)).thenReturn(updatedResponse);

        ResponseEntity<OrderResponse> response = ordersController.editOrder(orderId, mockUpdateRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Zaktualizowany opis", response.getBody().getDescription());

        verify(jwtService).extractUserId();
        verify(ordersService).editOrder(orderId, mockUpdateRequest, userId);
    }

    @Test
    public void givenUnauthorizedUser_whenEditOrder_thenThrowsException() {
        when(jwtService.extractUserId()).thenReturn(userId);
        doThrow(new InvalidCredentialsException())
                .when(ordersService).editOrder(orderId, mockUpdateRequest, userId);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ordersController.editOrder(orderId, mockUpdateRequest);
        });

        assertEquals("The provided credentials are invalid.", exception.getMessage());
        verify(ordersService).editOrder(orderId, mockUpdateRequest, userId);
    }

    @Test
    public void givenOrderInWrongStatus_whenEditOrder_thenThrowsException() {
        when(jwtService.extractUserId()).thenReturn(userId);
        doThrow(new OrderIsNotEditableException())
                .when(ordersService).editOrder(orderId, mockUpdateRequest, userId);

        RuntimeException exception = assertThrows(OrderIsNotEditableException.class, () -> {
            ordersController.editOrder(orderId, mockUpdateRequest);
        });

        assertEquals("Order is not editable because of status", exception.getMessage());
        verify(ordersService).editOrder(orderId, mockUpdateRequest, userId);
    }
}