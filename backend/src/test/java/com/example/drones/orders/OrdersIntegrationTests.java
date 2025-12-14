package com.example.drones.orders;

import com.example.drones.auth.dto.LoginRequest;
import com.example.drones.auth.dto.LoginResponse;
import com.example.drones.auth.dto.RegisterRequest;
import com.example.drones.orders.dto.OrderRequest;
import com.example.drones.orders.dto.OrderResponse;
import com.example.drones.orders.dto.OrderUpdateRequest;
import com.example.drones.services.ServicesEntity;
import com.example.drones.services.ServicesRepository;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OrdersIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private ServicesRepository servicesRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestRestTemplate testRestTemplate;

    private RegisterRequest userRegister;
    private LoginRequest userLogin;
    private final String SERVICE_NAME = "Laser Scanning";

    @BeforeEach
    void setUp() {
        if (!servicesRepository.existsById(SERVICE_NAME)) {
            ServicesEntity service = new ServicesEntity();
            service.setName(SERVICE_NAME);
            servicesRepository.save(service);
        }

        userRegister = RegisterRequest.builder()
                .displayName("testClient")
                .password("password123")
                .name("Jan")
                .surname("Testowy")
                .email("test@example.com")
                .phoneNumber("123456789")
                .build();

        userLogin = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();
    }

    @AfterEach
    void tearDown() {
        ordersRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerAndLogin() {
        testRestTemplate.postForEntity("/api/auth/register", userRegister, Void.class);
        ResponseEntity<LoginResponse> response = testRestTemplate.postForEntity("/api/auth/login", userLogin, LoginResponse.class);
        Assertions.assertNotNull(response.getBody());
        return response.getBody().token();
    }

    private HttpHeaders getHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-TOKEN", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void givenAuthenticatedUser_whenCreateOrder_thenOrderIsCreatedAndReturned() {
        String token = registerAndLogin();

        OrderRequest orderRequest = OrderRequest.builder()
                .title("Inspekcja Dachu")
                .description("Proszę o dokładne zdjęcia")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(2))
                .toDate(LocalDateTime.now().plusDays(3))
                .build();

        HttpEntity<OrderRequest> requestEntity = new HttpEntity<>(orderRequest, getHeaders(token));

        ResponseEntity<OrderResponse> response = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                requestEntity,
                OrderResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Assertions.assertNotNull(response.getBody());

        OrderResponse body = response.getBody();
        assertThat(body.getId()).isNotNull();
        assertThat(body.getTitle()).isEqualTo("Inspekcja Dachu");
        assertThat(body.getStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(body.getCreatedAt()).isNotNull();

        OrdersEntity savedOrder = ordersRepository.findById(body.getId()).orElseThrow();
        assertThat(savedOrder.getDescription()).isEqualTo("Proszę o dokładne zdjęcia");
        assertThat(savedOrder.getUser().getId()).isNotNull();

        UserEntity assignedUser = userRepository.findById(savedOrder.getUser().getId()).orElseThrow();
        assertThat(assignedUser.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void givenInvalidService_whenCreateOrder_thenReturnsInternalServerError() {
        String token = registerAndLogin();

        OrderRequest orderRequest = OrderRequest.builder()
                .title("Błędna usługa")
                .description("...")
                .service("NIEISTNIEJACA_USLUGA")
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        HttpEntity<OrderRequest> requestEntity = new HttpEntity<>(orderRequest, getHeaders(token));

        ResponseEntity<Void> response = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                requestEntity,
                Void.class
        );

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void givenValidPatchRequest_whenEditOrder_thenUpdatesDescriptionButNotTitle() {
        String token = registerAndLogin();

        OrderRequest createRequest = OrderRequest.builder()
                .title("Stary Tytuł")
                .description("Stary opis")
                .service(SERVICE_NAME)
                .coordinates("50, 50")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        HttpEntity<OrderRequest> createEntity = new HttpEntity<>(createRequest, getHeaders(token));
        ResponseEntity<OrderResponse> createResponse = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                createEntity,
                OrderResponse.class
        );
        UUID orderId = createResponse.getBody().getId();

        OrderUpdateRequest updateRequest = new OrderUpdateRequest();
        updateRequest.setDescription("NOWY OPIS");

        HttpEntity<OrderUpdateRequest> patchEntity = new HttpEntity<>(updateRequest, getHeaders(token));

        ResponseEntity<OrderResponse> patchResponse = testRestTemplate.exchange(
                "/api/orders/editOrder/" + orderId,
                HttpMethod.PATCH,
                patchEntity,
                OrderResponse.class
        );

        assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        OrderResponse body = patchResponse.getBody();

        assertThat(body.getDescription()).isEqualTo("NOWY OPIS");
        assertThat(body.getTitle()).isEqualTo("Stary Tytuł");

        OrdersEntity savedOrder = ordersRepository.findById(orderId).orElseThrow();
        assertThat(savedOrder.getDescription()).isEqualTo("NOWY OPIS");
    }

    @Test
    void givenAnotherUser_whenTryToEditOrder_thenReturnsInternalServerError() {
        String tokenUser1 = registerAndLogin();

        OrderRequest createRequest = OrderRequest.builder()
                .title("Zlecenie Usera 1")
                .description("...")
                .service(SERVICE_NAME)
                .coordinates("50,50")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        HttpEntity<OrderRequest> createEntity = new HttpEntity<>(createRequest, getHeaders(tokenUser1));
        ResponseEntity<OrderResponse> createResponse = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                createEntity,
                OrderResponse.class
        );
        UUID orderId = createResponse.getBody().getId();

        RegisterRequest user2Register = RegisterRequest.builder()
                .displayName("zlodziej")
                .password("pass")
                .name("Zly")
                .surname("Gosc")
                .email("zly@example.com")
                .phoneNumber("111222333")
                .build();
        testRestTemplate.postForEntity("/api/auth/register", user2Register, Void.class);

        LoginRequest user2Login = LoginRequest.builder()
                .email("zly@example.com")
                .password("pass")
                .build();
        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity("/api/auth/login", user2Login, LoginResponse.class);
        String tokenUser2 = loginResponse.getBody().token();

        OrderUpdateRequest updateRequest = new OrderUpdateRequest();
        updateRequest.setDescription("Hacked!");

        HttpEntity<OrderUpdateRequest> hackEntity = new HttpEntity<>(updateRequest, getHeaders(tokenUser2));

        ResponseEntity<Void> response = testRestTemplate.exchange(
                "/api/orders/editOrder/" + orderId,
                HttpMethod.PATCH,
                hackEntity,
                Void.class
        );

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();

        OrdersEntity orderInDb = ordersRepository.findById(orderId).orElseThrow();
    }
}