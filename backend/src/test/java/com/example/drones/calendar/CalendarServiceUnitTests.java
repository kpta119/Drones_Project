package com.example.drones.calendar;

import com.example.drones.calendar.exceptions.AddEventToCalendarException;
import com.example.drones.calendar.exceptions.OrderInProgressByOperatorIdNotFoundException;
import com.example.drones.calendar.exceptions.UserIsNotConnectedToGoogleException;
import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.orders.OrdersEntity;
import com.example.drones.orders.OrdersRepository;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CalendarServiceUnitTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrdersRepository ordersRepository;

    @InjectMocks
    private CalendarService calendarService;

    @Mock
    private OrdersEntity order;

    private UUID operatorId;
    private UUID orderId;
    private UserEntity operator;

    @BeforeEach
    public void setUp() {
        operatorId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        String refreshToken = "mock-refresh-token";

        operator = UserEntity.builder()
                .id(operatorId)
                .role(UserRole.OPERATOR)
                .email("operator@example.com")
                .name("John")
                .surname("Doe")
                .providerRefreshToken(refreshToken)
                .build();

        order = OrdersEntity.builder()
                .id(orderId)
                .title("Test Order")
                .description("Test Description")
                .fromDate(LocalDateTime.now())
                .toDate(LocalDateTime.now().plusDays(7))
                .build();

        ReflectionTestUtils.setField(calendarService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(calendarService, "clientSecret", "test-client-secret");
        ReflectionTestUtils.setField(calendarService, "frontendUrl", "http://localhost:3000");
    }

    @Test
    public void givenValidOperatorAndOrder_whenAddEvent_thenThrowsUserNotFoundException() {
        when(userRepository.findById(operatorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarService.addEvent(operatorId, orderId))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(operatorId);
        verify(ordersRepository, never()).findInProgressOrderByOperatorId(any(), any());
    }

    @Test
    public void givenOperatorWithoutRefreshToken_whenAddEvent_thenThrowsUserIsNotConnectedToGoogleException() {
        operator.setProviderRefreshToken(null);
        when(userRepository.findById(operatorId)).thenReturn(Optional.of(operator));

        assertThatThrownBy(() -> calendarService.addEvent(operatorId, orderId))
                .isInstanceOf(UserIsNotConnectedToGoogleException.class);

        verify(userRepository).findById(operatorId);
        verify(ordersRepository, never()).findInProgressOrderByOperatorId(any(), any());
    }

    @Test
    public void givenValidOperatorButOrderNotFound_whenAddEvent_thenThrowsOrderInProgressByOperatorIdNotFoundException() {
        when(userRepository.findById(operatorId)).thenReturn(Optional.of(operator));
        when(ordersRepository.findInProgressOrderByOperatorId(orderId, operatorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarService.addEvent(operatorId, orderId))
                .isInstanceOf(OrderInProgressByOperatorIdNotFoundException.class);

        verify(userRepository).findById(operatorId);
        verify(ordersRepository).findInProgressOrderByOperatorId(orderId, operatorId);
    }

    @Test
    public void givenValidInputs_whenAddEvent_thenCallsAddEventWithRefreshToken() {
        when(userRepository.findById(operatorId)).thenReturn(Optional.of(operator));
        when(ordersRepository.findInProgressOrderByOperatorId(orderId, operatorId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> calendarService.addEvent(operatorId, orderId))
                .isInstanceOf(AddEventToCalendarException.class);

        verify(userRepository).findById(operatorId);
        verify(ordersRepository).findInProgressOrderByOperatorId(orderId, operatorId);
    }

    @Test
    public void givenInvalidRefreshToken_whenAddEventWithRefreshToken_thenThrowsAddEventToCalendarException() {
        assertThatThrownBy(() -> calendarService.addEventWithRefreshToken("invalid-token", order))
                .isInstanceOf(AddEventToCalendarException.class);
    }

    @Test
    public void givenNullRefreshToken_whenAddEventWithRefreshToken_thenThrowsAddEventToCalendarException() {
        assertThatThrownBy(() -> calendarService.addEventWithRefreshToken(null, order))
                .isInstanceOf(AddEventToCalendarException.class);
    }

    @Test
    public void givenEmptyRefreshToken_whenAddEventWithRefreshToken_thenThrowsAddEventToCalendarException() {
        assertThatThrownBy(() -> calendarService.addEventWithRefreshToken("", order))
                .isInstanceOf(AddEventToCalendarException.class);
    }

    @Test
    public void givenValidOrder_whenPrepareEventObject_thenEventIdIsCorrectFormat() {
        String expectedPrefix = "order";
        String uuidWithoutDashes = orderId.toString().replace("-", "");
        String expectedEventId = expectedPrefix + uuidWithoutDashes;

        assertThat(expectedEventId).startsWith("order");
        assertThat(expectedEventId).doesNotContain("-");
        assertThat(expectedEventId.length()).isEqualTo(37);
    }

    @Test
    public void givenDifferentOrders_whenAddEvent_thenEachOrderIsHandledSeparately() {
        UUID order2Id = UUID.randomUUID();
        OrdersEntity order2 = OrdersEntity.builder()
                .id(order2Id)
                .title("Second Order")
                .description("Second Description")
                .fromDate(LocalDateTime.now())
                .toDate(LocalDateTime.now().plusDays(14))
                .build();

        when(userRepository.findById(operatorId)).thenReturn(Optional.of(operator));
        when(ordersRepository.findInProgressOrderByOperatorId(orderId, operatorId)).thenReturn(Optional.of(order));
        when(ordersRepository.findInProgressOrderByOperatorId(order2Id, operatorId)).thenReturn(Optional.of(order2));

        assertThatThrownBy(() -> calendarService.addEvent(operatorId, orderId))
                .isInstanceOf(AddEventToCalendarException.class);

        assertThatThrownBy(() -> calendarService.addEvent(operatorId, order2Id))
                .isInstanceOf(AddEventToCalendarException.class);

        verify(userRepository, times(2)).findById(operatorId);
        verify(ordersRepository).findInProgressOrderByOperatorId(orderId, operatorId);
        verify(ordersRepository).findInProgressOrderByOperatorId(order2Id, operatorId);
    }
}
