package com.example.drones.admin;

import com.example.drones.admin.dto.OrderDto;
import com.example.drones.admin.dto.SystemStatsDto;
import com.example.drones.admin.dto.UserDto;
import com.example.drones.orders.OrderStatus;
import com.example.drones.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminControllerTests {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private UserDto userDto1;
    private UserDto userDto2;

    @BeforeEach
    public void setUp() {

        userDto1 = new UserDto(
                UUID.randomUUID().toString(),
                "testUser1",
                UserRole.CLIENT,
                "John",
                "Doe",
                "john.doe@example.com",
                "1234567890"
        );

        userDto2 = new UserDto(
                UUID.randomUUID().toString(),
                "testUser2",
                UserRole.OPERATOR,
                "Jane",
                "Smith",
                "jane.smith@example.com",
                "0987654321"
        );
    }

    @Test
    public void givenNoFilters_whenGetUsers_thenReturnsPageOfUsers() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<UserDto> expectedPage = new PageImpl<>(List.of(userDto1, userDto2), pageable, 2);
        when(adminService.getUsers(null, null, pageable)).thenReturn(expectedPage);

        ResponseEntity<Page<UserDto>> response = adminController.getUsers(null, null, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(2);
        assertThat(response.getBody().getContent()).containsExactly(userDto1, userDto2);
        verify(adminService).getUsers(null, null, pageable);
    }

    @Test
    public void givenQueryParameter_whenGetUsers_thenReturnsFilteredUsers() {
        String query = "john";
        Pageable pageable = PageRequest.of(0, 20);
        Page<UserDto> expectedPage = new PageImpl<>(List.of(userDto1), pageable, 1);
        when(adminService.getUsers(query, null, pageable)).thenReturn(expectedPage);

        ResponseEntity<Page<UserDto>> response = adminController.getUsers(query, null, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent()).containsExactly(userDto1);
        verify(adminService).getUsers(query, null, pageable);
    }

    @Test
    public void givenPageable_whenGetOrder_thenReturnsPageOfOrders() {
        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        OrderDto orderDto1 = new OrderDto(
                orderId1,
                "Aerial Photography",
                "Professional aerial photography for real estate",
                "Photography",
                "52.2297,21.0122",
                now.plusDays(1),
                now.plusDays(2),
                OrderStatus.OPEN,
                now,
                clientId,
                null
        );

        OrderDto orderDto2 = new OrderDto(
                orderId2,
                "Land Survey",
                "Topographic survey of construction site",
                "Surveying",
                "52.2400,21.0300",
                now.plusDays(3),
                now.plusDays(4),
                OrderStatus.IN_PROGRESS,
                now,
                clientId,
                operatorId
        );

        Pageable pageable = PageRequest.of(0, 20);
        Page<OrderDto> expectedPage = new PageImpl<>(List.of(orderDto1, orderDto2), pageable, 2);
        when(adminService.getOrders(pageable)).thenReturn(expectedPage);

        ResponseEntity<Page<OrderDto>> response = adminController.getOrder(pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(2);
        assertThat(response.getBody().getContent()).containsExactly(orderDto1, orderDto2);
        assertThat(response.getBody().getTotalElements()).isEqualTo(2);
        verify(adminService).getOrders(pageable);
    }

    @Test
    public void whenGetStats_thenReturnsSystemStatistics() {
        UUID topOperatorId = UUID.randomUUID();

        SystemStatsDto.UsersStats usersStats = SystemStatsDto.UsersStats.builder()
                .clients(730L)
                .operators(270L)
                .build();

        SystemStatsDto.OrdersStats ordersStats = SystemStatsDto.OrdersStats.builder()
                .active(52L)
                .completed(248L)
                .avgPerOperator(3.2)
                .build();

        SystemStatsDto.TopOperator topOperator = SystemStatsDto.TopOperator.builder()
                .operatorId(topOperatorId)
                .completedOrders(57L)
                .build();

        SystemStatsDto.OperatorsStats operatorsStats = SystemStatsDto.OperatorsStats.builder()
                .busy(18L)
                .topOperator(topOperator)
                .build();

        SystemStatsDto.ReviewsStats reviewsStats = SystemStatsDto.ReviewsStats.builder()
                .total(415L)
                .build();

        SystemStatsDto expectedStats = SystemStatsDto.builder()
                .users(usersStats)
                .orders(ordersStats)
                .operators(operatorsStats)
                .reviews(reviewsStats)
                .build();

        when(adminService.getSystemStats()).thenReturn(expectedStats);

        ResponseEntity<SystemStatsDto> response = adminController.getStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsers().getClients()).isEqualTo(730L);
        assertThat(response.getBody().getUsers().getOperators()).isEqualTo(270L);
        assertThat(response.getBody().getOrders().getActive()).isEqualTo(52L);
        assertThat(response.getBody().getOrders().getCompleted()).isEqualTo(248L);
        assertThat(response.getBody().getOrders().getAvgPerOperator()).isEqualTo(3.2);
        assertThat(response.getBody().getOperators().getBusy()).isEqualTo(18L);
        assertThat(response.getBody().getOperators().getTopOperator()).isNotNull();
        assertThat(response.getBody().getOperators().getTopOperator().getOperatorId()).isEqualTo(topOperatorId);
        assertThat(response.getBody().getOperators().getTopOperator().getCompletedOrders()).isEqualTo(57L);
        assertThat(response.getBody().getReviews().getTotal()).isEqualTo(415L);
        verify(adminService).getSystemStats();
    }

}
