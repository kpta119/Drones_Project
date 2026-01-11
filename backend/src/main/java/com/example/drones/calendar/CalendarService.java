package com.example.drones.calendar;

import com.example.drones.calendar.dto.SchedulableOrders;
import com.example.drones.calendar.exceptions.AddEventToCalendarException;
import com.example.drones.calendar.exceptions.OrderInProgressByOperatorIdNotFoundException;
import com.example.drones.calendar.exceptions.UserIsNotConnectedToGoogleException;
import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.orders.OrdersEntity;
import com.example.drones.orders.OrdersMapper;
import com.example.drones.orders.OrdersRepository;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarService {
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final UserRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final OrdersMapper ordersMapper;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${app.frontend_url}")
    private String frontendUrl;

    public String addEvent(UUID operatorId, UUID orderId) {
        UserEntity user = userRepository.findById(operatorId)
                .orElseThrow(UserNotFoundException::new);
        String userRefreshToken = user.getProviderRefreshToken();
        if (userRefreshToken == null) {
            throw new UserIsNotConnectedToGoogleException();
        }
        OrdersEntity order = ordersRepository.findInProgressOrderByOperatorId(orderId, operatorId)
                .orElseThrow(OrderInProgressByOperatorIdNotFoundException::new);

        return addEventWithRefreshToken(userRefreshToken, order);
    }

    public String addEventWithRefreshToken(String userRefreshToken, OrdersEntity order) {
        String googleEventId = createGoogleEventId(order.getId());
        Event event = prepareEventObject(order, googleEventId);

        try {
            Calendar service = buildCalendarService(userRefreshToken);

            try {
                return service.events()
                        .insert("primary", event)
                        .execute()
                        .getHtmlLink();

            } catch (GoogleJsonResponseException e) {
                if (e.getStatusCode() == 409) {
                    log.info("Wydarzenie {} już istnieje. Aktualizuję.", googleEventId);
                    return service.events()
                            .update("primary", googleEventId, event)
                            .execute()
                            .getHtmlLink();
                }
                throw e;
            }

        } catch (Exception e) {
            log.warn(e.getMessage());
            throw new AddEventToCalendarException();

        }
    }

    public String createGoogleEventId(UUID orderId) {
        return "order" + orderId.toString().replace("-", "");
    }


    protected Calendar buildCalendarService(String refreshToken) throws GeneralSecurityException, IOException {
        UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(refreshToken)
                .build();

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                new HttpCredentialsAdapter(credentials)
        )
                .setApplicationName("DRONEX")
                .build();
    }

    protected Event prepareEventObject(OrdersEntity order, String googleEventId) {
        LocalDate eventDate = LocalDate.from(order.getToDate());

        Event event = new Event()
                .setId(googleEventId)
                .setSummary("DEADLINE zlecenia: " + order.getTitle())
                .setDescription("Szczegóły na: " + frontendUrl)
                .setColorId("11")
                .setTransparency("transparent");

        event.setStart(new EventDateTime().setDate(new DateTime(eventDate.toString())));
        event.setEnd(new EventDateTime().setDate(new DateTime(eventDate.plusDays(1).toString())));

        return event;
    }

    public Page<SchedulableOrders> getInProgressSchedulableOrders(UUID operatorId, Pageable pageable) {
        UserEntity user = userRepository.findById(operatorId)
                .orElseThrow(UserNotFoundException::new);


        Page<OrdersEntity> orders = ordersRepository
                .findInProgressOrdersByOperatorId(operatorId, pageable);

        Page<SchedulableOrders> schedulableOrders = orders.map(ordersMapper::toSchedulableOrders);

        markOrdersAddedToCalendar(schedulableOrders.getContent(), user.getProviderRefreshToken());
        return schedulableOrders;

    }

    public void markOrdersAddedToCalendar(List<SchedulableOrders> orders, String userRefreshToken) {
        if (userRefreshToken == null) {
            return;
        }
        if (orders.isEmpty()) return;

        try {
            Calendar service = buildCalendarService(userRefreshToken);

            LocalDateTime minDate = orders.stream()
                    .map(SchedulableOrders::getFromDate)
                    .min(LocalDateTime::compareTo)
                    .orElseThrow();

            LocalDateTime maxDate = orders.stream()
                    .map(SchedulableOrders::getToDate)
                    .max(LocalDateTime::compareTo)
                    .orElseThrow();

            Events eventsResult = service.events().list("primary")
                    .setTimeMin(new DateTime(minDate.minusDays(1).toString()))
                    .setTimeMax(new DateTime(maxDate.plusDays(1).toString()))
                    .setSingleEvents(true)
                    .setShowDeleted(false)
                    .execute();

            Set<String> googleEventIds = eventsResult.getItems().stream()
                    .map(Event::getId)
                    .collect(Collectors.toSet());

            for (SchedulableOrders order : orders) {
                String expectedGoogleId = createGoogleEventId(order.getId());
                boolean exists = googleEventIds.contains(expectedGoogleId);

                order.setAlreadyAdded(exists);
            }

        } catch (IOException | GeneralSecurityException e) {
            log.error("Failed to fetch event status from calendar", e);
            orders.forEach(o -> o.setAlreadyAdded(false));
        }
    }
}
