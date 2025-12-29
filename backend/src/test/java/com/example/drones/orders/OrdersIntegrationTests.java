package com.example.drones.orders;

import com.example.drones.auth.dto.LoginRequest;
import com.example.drones.auth.dto.LoginResponse;
import com.example.drones.auth.dto.RegisterRequest;
import com.example.drones.orders.dto.OrderRequest;
import com.example.drones.orders.dto.OrderResponse;
import com.example.drones.orders.dto.OrderUpdateRequest;
import com.example.drones.services.OperatorServicesEntity;
import com.example.drones.services.OperatorServicesRepository;
import com.example.drones.services.ServicesEntity;
import com.example.drones.services.ServicesRepository;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
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
import java.util.List;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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
    private OperatorServicesRepository operatorServicesRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NewMatchedOrdersRepository newMatchedOrdersRepository;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

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
        Assertions.assertNotNull(createResponse.getBody());
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

        Assertions.assertNotNull(body);
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
        Assertions.assertNotNull(createResponse.getBody());
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
        Assertions.assertNotNull(loginResponse.getBody());
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

        ordersRepository.findById(orderId).orElseThrow();
    }

    @Test
    void givenOrderRequest_whenCreated_thenMatchesOperatorsAutomatically() {
        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElse(null);
        if (service == null) {
            service = new ServicesEntity();
            service.setName(SERVICE_NAME);
            servicesRepository.save(service);
        }

        createTestOperator("op_match", "52.2200, 21.0100", 20, service);

        createTestOperator("op_far", "50.0647, 19.9450", 10, service);

        String clientToken = registerAndLogin();

        OrderRequest orderRequest = OrderRequest.builder()
                .title("Szukam drona")
                .description("Opis")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        HttpEntity<OrderRequest> requestEntity = new HttpEntity<>(orderRequest, getHeaders(clientToken));

        ResponseEntity<OrderResponse> response = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                requestEntity,
                OrderResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Assertions.assertNotNull(response.getBody());
        UUID createdOrderId = response.getBody().getId();

        await().atMost(5, SECONDS)
                .pollInterval(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {

                    List<NewMatchedOrderEntity> matches = newMatchedOrdersRepository.findAll();

                    List<NewMatchedOrderEntity> matchesForOrder = matches.stream()
                            .filter(m -> m.getOrder().getId().equals(createdOrderId))
                            .toList();

                    assertThat(matchesForOrder).hasSize(1);
                    NewMatchedOrderEntity match = matchesForOrder.getFirst();

                    UUID operatorId = match.getOperator().getId();
                    assertThat(operatorId).isNotNull();

                    UserEntity operatorFromDb = userRepository.findById(operatorId).orElseThrow();
                    assertThat(operatorFromDb.getDisplayName()).isEqualTo("op_match");
                    assertThat(matchesForOrder.getFirst().getOperatorStatus()).isEqualTo(MatchedOrderStatus.PENDING);
                    assertThat(matchesForOrder.getFirst().getClientStatus()).isEqualTo(MatchedOrderStatus.PENDING);
                });
    }

    private UserEntity createTestOperator(String username, String coords, int radius, ServicesEntity service) {
        UserEntity operator = UserEntity.builder()
                .displayName(username)
                .email(username + "@op.pl")
                .password(passwordEncoder.encode("pass"))
                .role(UserRole.OPERATOR)
                .name("Op").surname("Erator")
                .coordinates(coords)
                .radius(radius)
                .build();

        userRepository.save(operator);

        OperatorServicesEntity link = new OperatorServicesEntity();
        link.setOperator(operator);
        link.setServiceName(service.getName());
        operatorServicesRepository.save(link);

        return operator;
    }

    @Test
    void givenMatchedOrder_whenOperatorAcceptsOrder_thenStatusIsUpdatedToAwaitingOperator() {
        String clientToken = registerAndLogin();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator1", "52.2200, 21.0100", 20, service);

        OrderRequest orderRequest = OrderRequest.builder()
                .title("Test Order")
                .description("Test description")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        HttpEntity<OrderRequest> createEntity = new HttpEntity<>(orderRequest, getHeaders(clientToken));
        ResponseEntity<OrderResponse> createResponse = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                createEntity,
                OrderResponse.class
        );
        Assertions.assertNotNull(createResponse.getBody());
        UUID orderId = createResponse.getBody().getId();

        await().atMost(5, SECONDS).untilAsserted(() -> {
            List<NewMatchedOrderEntity> matches = newMatchedOrdersRepository
                    .findAll().stream()
                    .filter(m -> m.getOrder().getId().equals(orderId))
                    .toList();
            assertThat(matches).hasSize(1);
        });

        LoginRequest operatorLogin = LoginRequest.builder()
                .email("operator1@op.pl")
                .password("pass")
                .build();
        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login", operatorLogin, LoginResponse.class);
        Assertions.assertNotNull(loginResponse.getBody());
        String operatorToken = loginResponse.getBody().token();

        HttpEntity<Void> acceptEntity = new HttpEntity<>(getHeaders(operatorToken));
        ResponseEntity<OrderResponse> acceptResponse = testRestTemplate.exchange(
                "/api/orders/acceptOrder/" + orderId,
                HttpMethod.PATCH,
                acceptEntity,
                OrderResponse.class
        );

        // Then: Status zamówienia jest zmieniony na AWAITING_OPERATOR
        assertThat(acceptResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertNotNull(acceptResponse.getBody());
        assertThat(acceptResponse.getBody().getStatus()).isEqualTo(OrderStatus.AWAITING_OPERATOR);

        // Weryfikacja w bazie danych
        NewMatchedOrderEntity match = newMatchedOrdersRepository
                .findByOrderIdAndOperatorId(orderId, operator.getId())
                .orElseThrow();
        assertThat(match.getOperatorStatus()).isEqualTo(MatchedOrderStatus.ACCEPTED);
        assertThat(match.getClientStatus()).isEqualTo(MatchedOrderStatus.PENDING);
    }

    @Test
    void givenMatchedOrder_whenClientAcceptsOperator_thenStatusIsUpdatedToInProgress() {
        // Given: Utworzenie klienta i operatora
        String clientToken = registerAndLogin();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator2", "52.2200, 21.0100", 20, service);

        // Utworzenie zamówienia
        OrderRequest orderRequest = OrderRequest.builder()
                .title("Test Order 2")
                .description("Test description 2")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        HttpEntity<OrderRequest> createEntity = new HttpEntity<>(orderRequest, getHeaders(clientToken));
        ResponseEntity<OrderResponse> createResponse = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                createEntity,
                OrderResponse.class
        );
        Assertions.assertNotNull(createResponse.getBody());
        UUID orderId = createResponse.getBody().getId();

        // Czekanie na dopasowanie
        await().atMost(5, SECONDS).untilAsserted(() -> assertThat(newMatchedOrdersRepository.findByOrderIdAndOperatorId(orderId, operator.getId()))
                .isPresent());

        // Operator akceptuje zamówienie
        LoginRequest operatorLogin = LoginRequest.builder()
                .email("operator2@op.pl")
                .password("pass")
                .build();
        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login", operatorLogin, LoginResponse.class);
        Assertions.assertNotNull(loginResponse.getBody());
        String operatorToken = loginResponse.getBody().token();

        HttpEntity<Void> operatorAcceptEntity = new HttpEntity<>(getHeaders(operatorToken));
        testRestTemplate.exchange(
                "/api/orders/acceptOrder/" + orderId,
                HttpMethod.PATCH,
                operatorAcceptEntity,
                OrderResponse.class
        );

        // When: Klient akceptuje operatora
        HttpEntity<Void> clientAcceptEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<OrderResponse> acceptResponse = testRestTemplate.exchange(
                "/api/orders/acceptOrder/" + orderId + "?operatorId=" + operator.getId(),
                HttpMethod.PATCH,
                clientAcceptEntity,
                OrderResponse.class
        );

        // Then: Status zamówienia jest zmieniony na IN_PROGRESS
        assertThat(acceptResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertNotNull(acceptResponse.getBody());
        assertThat(acceptResponse.getBody().getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);

        // Weryfikacja w bazie danych
        NewMatchedOrderEntity match = newMatchedOrdersRepository
                .findByOrderIdAndOperatorId(orderId, operator.getId())
                .orElseThrow();
        assertThat(match.getOperatorStatus()).isEqualTo(MatchedOrderStatus.ACCEPTED);
        assertThat(match.getClientStatus()).isEqualTo(MatchedOrderStatus.ACCEPTED);
    }

    @Test
    void givenNonOperator_whenTriesToAcceptOrder_thenReturnsError() {
        // Given: Dwóch klientów (nie operatorów)
        String client1Token = registerAndLogin();

        // Utworzenie zamówienia przez pierwszego klienta
        OrderRequest orderRequest = OrderRequest.builder()
                .title("Test Order")
                .description("Test description")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        HttpEntity<OrderRequest> createEntity = new HttpEntity<>(orderRequest, getHeaders(client1Token));
        ResponseEntity<OrderResponse> createResponse = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                createEntity,
                OrderResponse.class
        );
        Assertions.assertNotNull(createResponse.getBody());
        UUID orderId = createResponse.getBody().getId();

        // Drugi klient próbuje zaakceptować zamówienie
        RegisterRequest client2Register = RegisterRequest.builder()
                .displayName("client2")
                .password("password456")
                .name("Anna")
                .surname("Kowalska")
                .email("client2@example.com")
                .phoneNumber("987654321")
                .build();
        testRestTemplate.postForEntity("/api/auth/register", client2Register, Void.class);

        LoginRequest client2Login = LoginRequest.builder()
                .email("client2@example.com")
                .password("password456")
                .build();
        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login", client2Login, LoginResponse.class);
        Assertions.assertNotNull(loginResponse.getBody());
        String client2Token = loginResponse.getBody().token();

        // When: Klient (nie operator) próbuje zaakceptować zamówienie
        HttpEntity<Void> acceptEntity = new HttpEntity<>(getHeaders(client2Token));
        ResponseEntity<Void> acceptResponse = testRestTemplate.exchange(
                "/api/orders/acceptOrder/" + orderId,
                HttpMethod.PATCH,
                acceptEntity,
                Void.class
        );

        // Then: Zwraca błąd
        assertThat(acceptResponse.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void givenOwnOrder_whenOperatorTriesToAcceptIt_thenReturnsError() {
        // Given: Użytkownik będący operatorem tworzy zamówienie
        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();

        UserEntity operatorClient = UserEntity.builder()
                .displayName("operator_client")
                .email("operator_client@example.com")
                .password("pass123")
                .role(UserRole.OPERATOR)
                .name("Jan")
                .surname("Operator")
                .coordinates("52.23, 21.01")
                .radius(20)
                .build();
        userRepository.save(operatorClient);

        OperatorServicesEntity link = new OperatorServicesEntity();
        link.setOperator(operatorClient);
        link.setServiceName(service.getName());
        operatorServicesRepository.save(link);

        LoginRequest login = LoginRequest.builder()
                .email("operator_client@example.com")
                .password("pass123")
                .build();
        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login", login, LoginResponse.class);
        Assertions.assertNotNull(loginResponse.getBody());
        String token = loginResponse.getBody().token();

        // Utworzenie zamówienia
        OrderRequest orderRequest = OrderRequest.builder()
                .title("Own Order")
                .description("My own order")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        HttpEntity<OrderRequest> createEntity = new HttpEntity<>(orderRequest, getHeaders(token));
        ResponseEntity<OrderResponse> createResponse = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                createEntity,
                OrderResponse.class
        );
        Assertions.assertNotNull(createResponse.getBody());
        UUID orderId = createResponse.getBody().getId();

        // When: Operator próbuje zaakceptować własne zamówienie
        HttpEntity<Void> acceptEntity = new HttpEntity<>(getHeaders(token));
        ResponseEntity<Void> acceptResponse = testRestTemplate.exchange(
                "/api/orders/acceptOrder/" + orderId,
                HttpMethod.PATCH,
                acceptEntity,
                Void.class
        );

        // Then: Zwraca błąd
        assertThat(acceptResponse.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void givenNotMatchedOperator_whenTriesToAcceptOrder_thenReturnsError() {
        // Given: Klient tworzy zamówienie
        String clientToken = registerAndLogin();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();

        OrderRequest orderRequest = OrderRequest.builder()
                .title("Test Order")
                .description("Test description")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        HttpEntity<OrderRequest> createEntity = new HttpEntity<>(orderRequest, getHeaders(clientToken));
        ResponseEntity<OrderResponse> createResponse = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                createEntity,
                OrderResponse.class
        );
        Assertions.assertNotNull(createResponse.getBody());
        UUID orderId = createResponse.getBody().getId();

        // Operator, który nie został dopasowany do zamówienia
        UserEntity unmatchedOperator = UserEntity.builder()
                .displayName("unmatched_op")
                .email("unmatched@op.pl")
                .password("pass")
                .role(UserRole.OPERATOR)
                .name("Un").surname("Matched")
                .coordinates("50.0647, 19.9450") // Daleko od zamówienia
                .radius(5)
                .build();
        userRepository.save(unmatchedOperator);

        OperatorServicesEntity link = new OperatorServicesEntity();
        link.setOperator(unmatchedOperator);
        link.setServiceName(service.getName());
        operatorServicesRepository.save(link);

        LoginRequest operatorLogin = LoginRequest.builder()
                .email("unmatched@op.pl")
                .password("pass")
                .build();
        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login", operatorLogin, LoginResponse.class);
        Assertions.assertNotNull(loginResponse.getBody());
        String operatorToken = loginResponse.getBody().token();

        // When: Niedopasowany operator próbuje zaakceptować zamówienie
        HttpEntity<Void> acceptEntity = new HttpEntity<>(getHeaders(operatorToken));
        ResponseEntity<Void> acceptResponse = testRestTemplate.exchange(
                "/api/orders/acceptOrder/" + orderId,
                HttpMethod.PATCH,
                acceptEntity,
                Void.class
        );

        // Then: Zwraca błąd
        assertThat(acceptResponse.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void givenNotOrderOwner_whenTriesToAcceptOperator_thenReturnsError() {
        // Given: Klient1 tworzy zamówienie
        String client1Token = registerAndLogin();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator3", "52.2200, 21.0100", 20, service);

        OrderRequest orderRequest = OrderRequest.builder()
                .title("Test Order")
                .description("Test description")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        HttpEntity<OrderRequest> createEntity = new HttpEntity<>(orderRequest, getHeaders(client1Token));
        ResponseEntity<OrderResponse> createResponse = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                createEntity,
                OrderResponse.class
        );
        Assertions.assertNotNull(createResponse.getBody());
        UUID orderId = createResponse.getBody().getId();

        // Czekanie na dopasowanie
        await().atMost(5, SECONDS).untilAsserted(() -> assertThat(newMatchedOrdersRepository.findByOrderIdAndOperatorId(orderId, operator.getId()))
                .isPresent());

        // Klient2 (nie właściciel zamówienia)
        RegisterRequest client2Register = RegisterRequest.builder()
                .displayName("client2")
                .password("password456")
                .name("Anna")
                .surname("Kowalska")
                .email("client2@example.com")
                .phoneNumber("987654321")
                .build();
        testRestTemplate.postForEntity("/api/auth/register", client2Register, Void.class);

        LoginRequest client2Login = LoginRequest.builder()
                .email("client2@example.com")
                .password("password456")
                .build();
        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login", client2Login, LoginResponse.class);
        Assertions.assertNotNull(loginResponse.getBody());
        String client2Token = loginResponse.getBody().token();

        // When: Klient2 próbuje zaakceptować operatora dla zamówienia Klienta1
        HttpEntity<Void> acceptEntity = new HttpEntity<>(getHeaders(client2Token));
        ResponseEntity<Void> acceptResponse = testRestTemplate.exchange(
                "/api/orders/acceptOrder/" + orderId + "?operatorId=" + operator.getId(),
                HttpMethod.PATCH,
                acceptEntity,
                Void.class
        );

        // Then: Zwraca błąd
        assertThat(acceptResponse.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void givenNonExistentOrder_whenTriesToAccept_thenReturnsError() {
        // Given: Operator z tokenem
        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        createTestOperator("operator4", "52.2200, 21.0100", 20, service);

        LoginRequest operatorLogin = LoginRequest.builder()
                .email("operator4@op.pl")
                .password("pass")
                .build();
        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login", operatorLogin, LoginResponse.class);
        Assertions.assertNotNull(loginResponse.getBody());
        String operatorToken = loginResponse.getBody().token();

        // When: Operator próbuje zaakceptować nieistniejące zamówienie
        UUID fakeOrderId = UUID.randomUUID();
        HttpEntity<Void> acceptEntity = new HttpEntity<>(getHeaders(operatorToken));
        ResponseEntity<Void> acceptResponse = testRestTemplate.exchange(
                "/api/orders/acceptOrder/" + fakeOrderId,
                HttpMethod.PATCH,
                acceptEntity,
                Void.class
        );

        // Then: Zwraca błąd
        assertThat(acceptResponse.getStatusCode().is4xxClientError()).isTrue();
    }
}