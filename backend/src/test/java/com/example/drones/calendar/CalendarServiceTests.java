package com.example.drones.calendar;

import com.example.drones.calendar.dto.SchedulableOrders;
import com.example.drones.calendar.exceptions.OrderInProgressByOperatorIdNotFoundException;
import com.example.drones.calendar.exceptions.UserIsNotConnectedToGoogleException;
import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.orders.OrdersEntity;
import com.example.drones.orders.OrdersMapper;
import com.example.drones.orders.OrdersRepository;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CalendarServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrdersRepository ordersRepository;

    @Mock
    private OrdersMapper ordersMapper;

    @Spy
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
    public void givenValidOrder_whenPrepareEventObject_thenEventIdIsCorrectFormat() {
        String expectedPrefix = "order";
        String uuidWithoutDashes = orderId.toString().replace("-", "");
        String expectedEventId = expectedPrefix + uuidWithoutDashes;

        assertThat(expectedEventId).startsWith("order");
        assertThat(expectedEventId).doesNotContain("-");
        assertThat(expectedEventId.length()).isEqualTo(37);
    }

    @Test
    public void givenValidOperatorId_whenGetInProgressSchedulableOrders_thenReturnsSchedulableOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        List<OrdersEntity> ordersList = List.of(order);
        Page<OrdersEntity> ordersPage = new PageImpl<>(ordersList, pageable, 1);

        SchedulableOrders schedulableOrder = SchedulableOrders.builder()
                .id(orderId)
                .title("Test Order")
                .alreadyAdded(false)
                .fromDate(LocalDateTime.now())
                .toDate(LocalDateTime.now().plusDays(7))
                .build();

        when(userRepository.findById(operatorId)).thenReturn(Optional.of(operator));
        when(ordersRepository.findInProgressOrdersByOperatorId(operatorId, pageable)).thenReturn(ordersPage);
        when(ordersMapper.toSchedulableOrders(order)).thenReturn(schedulableOrder);

        // Mock markOrdersAddedToCalendar żeby nie wywoływać Google API
        doNothing().when(calendarService).markOrdersAddedToCalendar(anyList(), anyString());

        Page<SchedulableOrders> result = calendarService.getInProgressSchedulableOrders(operatorId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(orderId);

        verify(userRepository).findById(operatorId);
        verify(ordersRepository).findInProgressOrdersByOperatorId(operatorId, pageable);
        verify(ordersMapper).toSchedulableOrders(order);
    }

    @Test
    public void givenOperatorNotFound_whenGetInProgressSchedulableOrders_thenThrowsUserNotFoundException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findById(operatorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarService.getInProgressSchedulableOrders(operatorId, pageable))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(operatorId);
        verify(ordersRepository, never()).findInProgressOrdersByOperatorId(any(), any());
    }

    @Test
    public void givenOperatorWithoutRefreshToken_whenGetInProgressSchedulableOrders_thenReturnsOrdersWithoutCalendarCheck() {
        Pageable pageable = PageRequest.of(0, 10);
        operator.setProviderRefreshToken(null);

        SchedulableOrders schedulableOrder = SchedulableOrders.builder()
                .id(orderId)
                .title("Test Order")
                .fromDate(LocalDateTime.now())
                .toDate(LocalDateTime.now().plusDays(7))
                .build();

        List<OrdersEntity> ordersList = List.of(order);
        Page<OrdersEntity> ordersPage = new PageImpl<>(ordersList, pageable, 1);

        when(userRepository.findById(operatorId)).thenReturn(Optional.of(operator));
        when(ordersRepository.findInProgressOrdersByOperatorId(operatorId, pageable)).thenReturn(ordersPage);
        when(ordersMapper.toSchedulableOrders(order)).thenReturn(schedulableOrder);

        // Mock markOrdersAddedToCalendar żeby nie wywoływać Google API
        doNothing().when(calendarService).markOrdersAddedToCalendar(anyList(), isNull());

        Page<SchedulableOrders> result = calendarService.getInProgressSchedulableOrders(operatorId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        // Bez refresh token, markOrdersAddedToCalendar po prostu zwraca bez sprawdzania kalendarza

        verify(userRepository).findById(operatorId);
        verify(ordersRepository).findInProgressOrdersByOperatorId(operatorId, pageable);
    }

    @Test
    public void givenNoOrders_whenGetInProgressSchedulableOrders_thenReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<OrdersEntity> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(userRepository.findById(operatorId)).thenReturn(Optional.of(operator));
        when(ordersRepository.findInProgressOrdersByOperatorId(operatorId, pageable)).thenReturn(emptyPage);

        // Mock markOrdersAddedToCalendar żeby nie wywoływać Google API
        doNothing().when(calendarService).markOrdersAddedToCalendar(anyList(), anyString());

        Page<SchedulableOrders> result = calendarService.getInProgressSchedulableOrders(operatorId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);

        verify(userRepository).findById(operatorId);
        verify(ordersRepository).findInProgressOrdersByOperatorId(operatorId, pageable);
    }

    @Test
    public void givenMultipleOrders_whenGetInProgressSchedulableOrders_thenReturnsAllOrders() {
        Pageable pageable = PageRequest.of(0, 10);

        UUID order2Id = UUID.randomUUID();
        OrdersEntity order2 = OrdersEntity.builder()
                .id(order2Id)
                .title("Second Order")
                .fromDate(LocalDateTime.now())
                .toDate(LocalDateTime.now().plusDays(7))
                .build();

        UUID order3Id = UUID.randomUUID();
        OrdersEntity order3 = OrdersEntity.builder()
                .id(order3Id)
                .title("Third Order")
                .fromDate(LocalDateTime.now())
                .toDate(LocalDateTime.now().plusDays(7))
                .build();

        List<OrdersEntity> ordersList = List.of(order, order2, order3);
        Page<OrdersEntity> ordersPage = new PageImpl<>(ordersList, pageable, 3);

        SchedulableOrders schedulableOrder1 = SchedulableOrders.builder()
                .id(orderId)
                .fromDate(LocalDateTime.now())
                .toDate(LocalDateTime.now().plusDays(7))
                .build();
        SchedulableOrders schedulableOrder2 = SchedulableOrders.builder()
                .id(order2Id)
                .fromDate(LocalDateTime.now())
                .toDate(LocalDateTime.now().plusDays(7))
                .build();
        SchedulableOrders schedulableOrder3 = SchedulableOrders.builder()
                .id(order3Id)
                .fromDate(LocalDateTime.now())
                .toDate(LocalDateTime.now().plusDays(7))
                .build();

        when(userRepository.findById(operatorId)).thenReturn(Optional.of(operator));
        when(ordersRepository.findInProgressOrdersByOperatorId(operatorId, pageable)).thenReturn(ordersPage);
        when(ordersMapper.toSchedulableOrders(order)).thenReturn(schedulableOrder1);
        when(ordersMapper.toSchedulableOrders(order2)).thenReturn(schedulableOrder2);
        when(ordersMapper.toSchedulableOrders(order3)).thenReturn(schedulableOrder3);

        // Mock markOrdersAddedToCalendar żeby nie wywoływać Google API
        doNothing().when(calendarService).markOrdersAddedToCalendar(anyList(), anyString());

        Page<SchedulableOrders> result = calendarService.getInProgressSchedulableOrders(operatorId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);

        verify(ordersMapper, times(3)).toSchedulableOrders(any());
    }

    @Test
    public void givenEmptyOrdersList_whenMarkOrdersAddedToCalendar_thenDoesNothing() {
        List<SchedulableOrders> emptyList = Collections.emptyList();
        String refreshToken = "test-token";

        calendarService.markOrdersAddedToCalendar(emptyList, refreshToken);

        verifyNoInteractions(ordersRepository);
    }

    @Test
    public void givenNullRefreshToken_whenMarkOrdersAddedToCalendar_thenReturnsEarlyWithoutProcessing() {
        SchedulableOrders schedulableOrder = SchedulableOrders.builder()
                .id(orderId)
                .title("Test Order")
                .fromDate(LocalDateTime.now())
                .toDate(LocalDateTime.now().plusDays(7))
                .alreadyAdded(null)
                .build();

        List<SchedulableOrders> orders = List.of(schedulableOrder);

        // Null refresh token powoduje wczesny return bez rzucania wyjątku
        calendarService.markOrdersAddedToCalendar(orders, null);

        // Zamówienie powinno pozostać bez zmian (alreadyAdded = null)
        assertThat(schedulableOrder.getAlreadyAdded()).isNull();
    }

    @Test
    public void givenOrdersExistInGoogleCalendar_whenMarkOrdersAddedToCalendar_thenSetsAlreadyAddedToTrue() throws Exception {
        UUID order1Id = UUID.randomUUID();
        UUID order2Id = UUID.randomUUID();
        UUID order3Id = UUID.randomUUID();

        LocalDateTime now = LocalDateTime.now();

        SchedulableOrders order1 = SchedulableOrders.builder()
                .id(order1Id)
                .title("Order 1")
                .fromDate(now)
                .toDate(now.plusDays(5))
                .alreadyAdded(false)
                .build();

        SchedulableOrders order2 = SchedulableOrders.builder()
                .id(order2Id)
                .title("Order 2")
                .fromDate(now.plusDays(1))
                .toDate(now.plusDays(6))
                .alreadyAdded(false)
                .build();

        SchedulableOrders order3 = SchedulableOrders.builder()
                .id(order3Id)
                .title("Order 3")
                .fromDate(now.plusDays(2))
                .toDate(now.plusDays(7))
                .alreadyAdded(false)
                .build();

        List<SchedulableOrders> orders = Arrays.asList(order1, order2, order3);
        String refreshToken = "valid-token";

        Calendar mockCalendar = mock(Calendar.class);
        Calendar.Events mockEvents = mock(Calendar.Events.class);
        Calendar.Events.List mockList = mock(Calendar.Events.List.class);

        // Używamy calendarService który już jest @Spy
        doReturn(mockCalendar).when(calendarService).buildCalendarService(refreshToken);

        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.list("primary")).thenReturn(mockList);
        when(mockList.setTimeMin(any())).thenReturn(mockList);
        when(mockList.setTimeMax(any())).thenReturn(mockList);
        when(mockList.setSingleEvents(anyBoolean())).thenReturn(mockList);
        when(mockList.setShowDeleted(anyBoolean())).thenReturn(mockList);

        Events mockEventsResult = new Events();
        List<Event> existingEvents = new ArrayList<>();

        Event event1 = new Event();
        event1.setId("order" + order1Id.toString().replace("-", ""));
        existingEvents.add(event1);

        Event event2 = new Event();
        event2.setId("order" + order2Id.toString().replace("-", ""));
        existingEvents.add(event2);

        mockEventsResult.setItems(existingEvents);
        when(mockList.execute()).thenReturn(mockEventsResult);

        calendarService.markOrdersAddedToCalendar(orders, refreshToken);

        assertThat(order1.getAlreadyAdded()).isTrue();
        assertThat(order2.getAlreadyAdded()).isTrue();
        assertThat(order3.getAlreadyAdded()).isFalse();
    }

    @Test
    public void givenNoOrdersExistInGoogleCalendar_whenMarkOrdersAddedToCalendar_thenSetsAlreadyAddedToFalse() throws Exception {
        UUID order1Id = UUID.randomUUID();
        UUID order2Id = UUID.randomUUID();

        LocalDateTime now = LocalDateTime.now();

        SchedulableOrders order1 = SchedulableOrders.builder()
                .id(order1Id)
                .title("Order 1")
                .fromDate(now)
                .toDate(now.plusDays(5))
                .alreadyAdded(null)
                .build();

        SchedulableOrders order2 = SchedulableOrders.builder()
                .id(order2Id)
                .title("Order 2")
                .fromDate(now.plusDays(1))
                .toDate(now.plusDays(6))
                .alreadyAdded(null)
                .build();

        List<SchedulableOrders> orders = Arrays.asList(order1, order2);
        String refreshToken = "valid-token";

        Calendar mockCalendar = mock(Calendar.class);
        Calendar.Events mockEvents = mock(Calendar.Events.class);
        Calendar.Events.List mockList = mock(Calendar.Events.List.class);

        // Używamy calendarService który już jest @Spy
        doReturn(mockCalendar).when(calendarService).buildCalendarService(refreshToken);

        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.list("primary")).thenReturn(mockList);
        when(mockList.setTimeMin(any())).thenReturn(mockList);
        when(mockList.setTimeMax(any())).thenReturn(mockList);
        when(mockList.setSingleEvents(anyBoolean())).thenReturn(mockList);
        when(mockList.setShowDeleted(anyBoolean())).thenReturn(mockList);

        Events mockEventsResult = new Events();
        mockEventsResult.setItems(new ArrayList<>());
        when(mockList.execute()).thenReturn(mockEventsResult);

        calendarService.markOrdersAddedToCalendar(orders, refreshToken);

        assertThat(order1.getAlreadyAdded()).isFalse();
        assertThat(order2.getAlreadyAdded()).isFalse();
    }

    @Test
    public void givenGoogleEventIdsWithExtraEvents_whenMarkOrdersAddedToCalendar_thenOnlyMatchingOrdersMarked() throws Exception {
        UUID order1Id = UUID.randomUUID();

        LocalDateTime now = LocalDateTime.now();

        SchedulableOrders order1 = SchedulableOrders.builder()
                .id(order1Id)
                .title("Order 1")
                .fromDate(now)
                .toDate(now.plusDays(5))
                .build();

        List<SchedulableOrders> orders = List.of(order1);
        String refreshToken = "valid-token";

        Calendar mockCalendar = mock(Calendar.class);
        Calendar.Events mockEvents = mock(Calendar.Events.class);
        Calendar.Events.List mockList = mock(Calendar.Events.List.class);

        // Używamy calendarService który już jest @Spy
        doReturn(mockCalendar).when(calendarService).buildCalendarService(refreshToken);

        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.list("primary")).thenReturn(mockList);
        when(mockList.setTimeMin(any())).thenReturn(mockList);
        when(mockList.setTimeMax(any())).thenReturn(mockList);
        when(mockList.setSingleEvents(anyBoolean())).thenReturn(mockList);
        when(mockList.setShowDeleted(anyBoolean())).thenReturn(mockList);

        Events mockEventsResult = getMockEventsResult(order1Id);
        when(mockList.execute()).thenReturn(mockEventsResult);

        calendarService.markOrdersAddedToCalendar(orders, refreshToken);

        assertThat(order1.getAlreadyAdded()).isTrue();
    }

    private static @NonNull Events getMockEventsResult(UUID order1Id) {
        Events mockEventsResult = new Events();
        List<Event> existingEvents = new ArrayList<>();

        Event event1 = new Event();
        event1.setId("order" + order1Id.toString().replace("-", ""));
        existingEvents.add(event1);

        Event extraEvent1 = new Event();
        extraEvent1.setId("someOtherEvent123");
        existingEvents.add(extraEvent1);

        Event extraEvent2 = new Event();
        extraEvent2.setId("anotherEvent456");
        existingEvents.add(extraEvent2);

        mockEventsResult.setItems(existingEvents);
        return mockEventsResult;
    }
}
