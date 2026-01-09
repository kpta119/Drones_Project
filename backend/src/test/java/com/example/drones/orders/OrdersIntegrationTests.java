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
import org.springframework.core.ParameterizedTypeReference;
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

        assertThat(acceptResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertNotNull(acceptResponse.getBody());
        assertThat(acceptResponse.getBody().getStatus()).isEqualTo(OrderStatus.AWAITING_OPERATOR);

        NewMatchedOrderEntity match = newMatchedOrdersRepository
                .findByOrderIdAndOperatorId(orderId, operator.getId())
                .orElseThrow();
        assertThat(match.getOperatorStatus()).isEqualTo(MatchedOrderStatus.ACCEPTED);
        assertThat(match.getClientStatus()).isEqualTo(MatchedOrderStatus.PENDING);
    }

    @Test
    void givenMatchedOrder_whenClientAcceptsOperator_thenStatusIsUpdatedToInProgress() {
        String clientToken = registerAndLogin();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator2", "52.2200, 21.0100", 20, service);

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

        await().atMost(5, SECONDS).untilAsserted(() -> assertThat(newMatchedOrdersRepository.findByOrderIdAndOperatorId(orderId, operator.getId()))
                .isPresent());

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

        HttpEntity<Void> clientAcceptEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<OrderResponse> acceptResponse = testRestTemplate.exchange(
                "/api/orders/acceptOrder/" + orderId + "?operatorId=" + operator.getId(),
                HttpMethod.PATCH,
                clientAcceptEntity,
                OrderResponse.class
        );

        assertThat(acceptResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertNotNull(acceptResponse.getBody());
        assertThat(acceptResponse.getBody().getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);

        NewMatchedOrderEntity match = newMatchedOrdersRepository
                .findByOrderIdAndOperatorId(orderId, operator.getId())
                .orElseThrow();
        assertThat(match.getOperatorStatus()).isEqualTo(MatchedOrderStatus.ACCEPTED);
        assertThat(match.getClientStatus()).isEqualTo(MatchedOrderStatus.ACCEPTED);
    }

    @Test
    void givenNonOperator_whenTriesToAcceptOrder_thenReturnsError() {
        String client1Token = registerAndLogin();

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

        HttpEntity<Void> acceptEntity = new HttpEntity<>(getHeaders(client2Token));
        ResponseEntity<Void> acceptResponse = testRestTemplate.exchange(
                "/api/orders/acceptOrder/" + orderId,
                HttpMethod.PATCH,
                acceptEntity,
                Void.class
        );

        assertThat(acceptResponse.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void givenOwnOrder_whenOperatorTriesToAcceptIt_thenReturnsError() {
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

        UserEntity unmatchedOperator = UserEntity.builder()
                .displayName("unmatched_op")
                .email("unmatched@op.pl")
                .password("pass")
                .role(UserRole.OPERATOR)
                .name("Un").surname("Matched")
                .coordinates("50.0647, 19.9450") // Far from the order
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

        HttpEntity<Void> acceptEntity = new HttpEntity<>(getHeaders(operatorToken));
        ResponseEntity<Void> acceptResponse = testRestTemplate.exchange(
                "/api/orders/acceptOrder/" + orderId,
                HttpMethod.PATCH,
                acceptEntity,
                Void.class
        );

        assertThat(acceptResponse.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void givenNotOrderOwner_whenTriesToAcceptOperator_thenReturnsError() {
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

        await().atMost(5, SECONDS).untilAsserted(() -> assertThat(newMatchedOrdersRepository.findByOrderIdAndOperatorId(orderId, operator.getId()))
                .isPresent());

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

        HttpEntity<Void> acceptEntity = new HttpEntity<>(getHeaders(client2Token));
        ResponseEntity<Void> acceptResponse = testRestTemplate.exchange(
                "/api/orders/acceptOrder/" + orderId + "?operatorId=" + operator.getId(),
                HttpMethod.PATCH,
                acceptEntity,
                Void.class
        );

        assertThat(acceptResponse.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void givenNonExistentOrder_whenTriesToAccept_thenReturnsError() {
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

        UUID fakeOrderId = UUID.randomUUID();
        HttpEntity<Void> acceptEntity = new HttpEntity<>(getHeaders(operatorToken));
        ResponseEntity<Void> acceptResponse = testRestTemplate.exchange(
                "/api/orders/acceptOrder/" + fakeOrderId,
                HttpMethod.PATCH,
                acceptEntity,
                Void.class
        );

        assertThat(acceptResponse.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void givenMatchedOrder_whenOperatorRejectsOrder_thenOperatorStatusIsRejected() {
        String clientToken = registerAndLogin();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator_reject1", "52.2200, 21.0100", 20, service);

        OrderRequest orderRequest = OrderRequest.builder()
                .title("Test Order for Rejection")
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
                .email("operator_reject1@op.pl")
                .password("pass")
                .build();
        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login", operatorLogin, LoginResponse.class);
        Assertions.assertNotNull(loginResponse.getBody());
        String operatorToken = loginResponse.getBody().token();

        HttpEntity<Void> rejectEntity = new HttpEntity<>(getHeaders(operatorToken));
        ResponseEntity<Void> rejectResponse = testRestTemplate.exchange(
                "/api/orders/rejectOrder/" + orderId,
                HttpMethod.PATCH,
                rejectEntity,
                Void.class
        );

        assertThat(rejectResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        NewMatchedOrderEntity match = newMatchedOrdersRepository
                .findByOrderIdAndOperatorId(orderId, operator.getId())
                .orElseThrow();
        assertThat(match.getOperatorStatus()).isEqualTo(MatchedOrderStatus.REJECTED);
        assertThat(match.getClientStatus()).isEqualTo(MatchedOrderStatus.PENDING);
    }

    @Test
    void givenMatchedOrder_whenClientRejectsOperator_thenClientStatusIsRejected() {
        String clientToken = registerAndLogin();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator_reject2", "52.2200, 21.0100", 20, service);

        OrderRequest orderRequest = OrderRequest.builder()
                .title("Test Order for Client Rejection")
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

        await().atMost(5, SECONDS).untilAsserted(() -> assertThat(newMatchedOrdersRepository.findByOrderIdAndOperatorId(orderId, operator.getId()))
                .isPresent());

        HttpEntity<Void> rejectEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<Void> rejectResponse = testRestTemplate.exchange(
                "/api/orders/rejectOrder/" + orderId + "?operatorId=" + operator.getId(),
                HttpMethod.PATCH,
                rejectEntity,
                Void.class
        );

        assertThat(rejectResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        NewMatchedOrderEntity match = newMatchedOrdersRepository
                .findByOrderIdAndOperatorId(orderId, operator.getId())
                .orElseThrow();
        assertThat(match.getOperatorStatus()).isEqualTo(MatchedOrderStatus.PENDING);
        assertThat(match.getClientStatus()).isEqualTo(MatchedOrderStatus.REJECTED);
    }

    @Test
    void givenNonOperator_whenTriesToRejectOrder_thenReturnsError() {
        String client1Token = registerAndLogin();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        createTestOperator("operator_reject3", "52.2200, 21.0100", 20, service);

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

        RegisterRequest client2Register = RegisterRequest.builder()
                .displayName("client_reject")
                .password("password456")
                .name("Anna")
                .surname("Kowalska")
                .email("client_reject@example.com")
                .phoneNumber("987654321")
                .build();
        testRestTemplate.postForEntity("/api/auth/register", client2Register, Void.class);

        LoginRequest client2Login = LoginRequest.builder()
                .email("client_reject@example.com")
                .password("password456")
                .build();
        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login", client2Login, LoginResponse.class);
        Assertions.assertNotNull(loginResponse.getBody());
        String client2Token = loginResponse.getBody().token();

        HttpEntity<Void> rejectEntity = new HttpEntity<>(getHeaders(client2Token));
        ResponseEntity<Void> rejectResponse = testRestTemplate.exchange(
                "/api/orders/rejectOrder/" + orderId,
                HttpMethod.PATCH,
                rejectEntity,
                Void.class
        );

        assertThat(rejectResponse.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void givenNotOrderOwner_whenTriesToRejectOperator_thenReturnsError() {
        String client1Token = registerAndLogin();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator_reject4", "52.2200, 21.0100", 20, service);

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

        await().atMost(5, SECONDS).untilAsserted(() -> assertThat(newMatchedOrdersRepository.findByOrderIdAndOperatorId(orderId, operator.getId()))
                .isPresent());

        RegisterRequest client2Register = RegisterRequest.builder()
                .displayName("client_reject2")
                .password("password456")
                .name("Anna")
                .surname("Kowalska")
                .email("client_reject2@example.com")
                .phoneNumber("987654321")
                .build();
        testRestTemplate.postForEntity("/api/auth/register", client2Register, Void.class);

        LoginRequest client2Login = LoginRequest.builder()
                .email("client_reject2@example.com")
                .password("password456")
                .build();
        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login", client2Login, LoginResponse.class);
        Assertions.assertNotNull(loginResponse.getBody());
        String client2Token = loginResponse.getBody().token();

        HttpEntity<Void> rejectEntity = new HttpEntity<>(getHeaders(client2Token));
        ResponseEntity<Void> rejectResponse = testRestTemplate.exchange(
                "/api/orders/rejectOrder/" + orderId + "?operatorId=" + operator.getId(),
                HttpMethod.PATCH,
                rejectEntity,
                Void.class
        );

        assertThat(rejectResponse.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void givenNotMatchedOperator_whenTriesToRejectOrder_thenReturnsError() {
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

        UserEntity unmatchedOperator = UserEntity.builder()
                .displayName("unmatched_op_reject")
                .email("unmatched_reject@op.pl")
                .password(passwordEncoder.encode("pass"))
                .role(UserRole.OPERATOR)
                .name("Un").surname("Matched")
                .coordinates("50.0647, 19.9450") // Far away from the order
                .radius(5)
                .build();
        userRepository.save(unmatchedOperator);

        OperatorServicesEntity link = new OperatorServicesEntity();
        link.setOperator(unmatchedOperator);
        link.setServiceName(service.getName());
        operatorServicesRepository.save(link);

        LoginRequest operatorLogin = LoginRequest.builder()
                .email("unmatched_reject@op.pl")
                .password("pass")
                .build();
        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login", operatorLogin, LoginResponse.class);
        Assertions.assertNotNull(loginResponse.getBody());
        String operatorToken = loginResponse.getBody().token();

        HttpEntity<Void> rejectEntity = new HttpEntity<>(getHeaders(operatorToken));
        ResponseEntity<Void> rejectResponse = testRestTemplate.exchange(
                "/api/orders/rejectOrder/" + orderId,
                HttpMethod.PATCH,
                rejectEntity,
                Void.class
        );

        assertThat(rejectResponse.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void givenNonExistentOrder_whenTriesToReject_thenReturnsError() {
        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        createTestOperator("operator_reject5", "52.2200, 21.0100", 20, service);

        LoginRequest operatorLogin = LoginRequest.builder()
                .email("operator_reject5@op.pl")
                .password("pass")
                .build();
        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login", operatorLogin, LoginResponse.class);
        Assertions.assertNotNull(loginResponse.getBody());
        String operatorToken = loginResponse.getBody().token();

        UUID fakeOrderId = UUID.randomUUID();
        HttpEntity<Void> rejectEntity = new HttpEntity<>(getHeaders(operatorToken));
        ResponseEntity<Void> rejectResponse = testRestTemplate.exchange(
                "/api/orders/rejectOrder/" + fakeOrderId,
                HttpMethod.PATCH,
                rejectEntity,
                Void.class
        );

        assertThat(rejectResponse.getStatusCode().is4xxClientError()).isTrue();
    }


    @Test
    void givenValidOrder_whenOwnerCancelsOrder_thenStatusIsChangedToCancelled() {
        String clientToken = registerAndLogin();

        OrderRequest orderRequest = OrderRequest.builder()
                .title("Order to Cancel")
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

        HttpEntity<Void> cancelEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<OrderResponse> cancelResponse = testRestTemplate.exchange(
                "/api/orders/cancelOrder/" + orderId,
                HttpMethod.PATCH,
                cancelEntity,
                OrderResponse.class
        );

        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertNotNull(cancelResponse.getBody());
        assertThat(cancelResponse.getBody().getStatus()).isEqualTo(OrderStatus.CANCELLED);

        OrdersEntity orderInDb = ordersRepository.findById(orderId).orElseThrow();
        assertThat(orderInDb.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void givenNotOwner_whenTriesToCancelOrder_thenReturnsError() {
        String client1Token = registerAndLogin();

        OrderRequest orderRequest = OrderRequest.builder()
                .title("Order to Cancel")
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

        RegisterRequest client2Register = RegisterRequest.builder()
                .displayName("client_cancel")
                .password("password456")
                .name("Anna")
                .surname("Kowalska")
                .email("client_cancel@example.com")
                .phoneNumber("987654321")
                .build();
        testRestTemplate.postForEntity("/api/auth/register", client2Register, Void.class);

        LoginRequest client2Login = LoginRequest.builder()
                .email("client_cancel@example.com")
                .password("password456")
                .build();
        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login", client2Login, LoginResponse.class);
        Assertions.assertNotNull(loginResponse.getBody());
        String client2Token = loginResponse.getBody().token();

        HttpEntity<Void> cancelEntity = new HttpEntity<>(getHeaders(client2Token));
        ResponseEntity<OrderResponse> cancelResponse = testRestTemplate.exchange(
                "/api/orders/cancelOrder/" + orderId,
                HttpMethod.PATCH,
                cancelEntity,
                OrderResponse.class
        );

        assertThat(cancelResponse.getStatusCode().is4xxClientError()).isTrue();

        OrdersEntity orderInDb = ordersRepository.findById(orderId).orElseThrow();
        assertThat(orderInDb.getStatus()).isNotEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void givenCompletedOrder_whenOwnerTriesToCancel_thenReturnsError() {
        String clientToken = registerAndLogin();

        OrderRequest orderRequest = OrderRequest.builder()
                .title("Completed Order")
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

        OrdersEntity order = ordersRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.COMPLETED);
        ordersRepository.save(order);

        HttpEntity<Void> cancelEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<OrderResponse> cancelResponse = testRestTemplate.exchange(
                "/api/orders/cancelOrder/" + orderId,
                HttpMethod.PATCH,
                cancelEntity,
                OrderResponse.class
        );

        assertThat(cancelResponse.getStatusCode().is4xxClientError()).isTrue();

        OrdersEntity orderInDb = ordersRepository.findById(orderId).orElseThrow();
        assertThat(orderInDb.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void givenNonExistentOrder_whenTriesToCancel_thenReturnsError() {
        String clientToken = registerAndLogin();

        UUID fakeOrderId = UUID.randomUUID();
        HttpEntity<Void> cancelEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<OrderResponse> cancelResponse = testRestTemplate.exchange(
                "/api/orders/cancelOrder/" + fakeOrderId,
                HttpMethod.PATCH,
                cancelEntity,
                OrderResponse.class
        );

        assertThat(cancelResponse.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void givenOrderInProgress_whenOwnerCancelsOrder_thenStatusIsChangedToCancelled() {
        String clientToken = registerAndLogin();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator_cancel", "52.2200, 21.0100", 20, service);

        OrderRequest orderRequest = OrderRequest.builder()
                .title("In Progress Order")
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

        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(newMatchedOrdersRepository.findByOrderIdAndOperatorId(orderId, operator.getId()))
                        .isPresent()
        );

        LoginRequest operatorLogin = LoginRequest.builder()
                .email("operator_cancel@op.pl")
                .password("pass")
                .build();
        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login", operatorLogin, LoginResponse.class);
        Assertions.assertNotNull(loginResponse.getBody());
        String operatorToken = loginResponse.getBody().token();

        HttpEntity<Void> acceptEntity = new HttpEntity<>(getHeaders(operatorToken));
        testRestTemplate.exchange(
                "/api/orders/acceptOrder/" + orderId,
                HttpMethod.PATCH,
                acceptEntity,
                OrderResponse.class
        );

        HttpEntity<Void> clientAcceptEntity = new HttpEntity<>(getHeaders(clientToken));
        testRestTemplate.exchange(
                "/api/orders/acceptOrder/" + orderId + "?operatorId=" + operator.getId(),
                HttpMethod.PATCH,
                clientAcceptEntity,
                OrderResponse.class
        );

        HttpEntity<Void> cancelEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<OrderResponse> cancelResponse = testRestTemplate.exchange(
                "/api/orders/cancelOrder/" + orderId,
                HttpMethod.PATCH,
                cancelEntity,
                OrderResponse.class
        );

        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertNotNull(cancelResponse.getBody());
        assertThat(cancelResponse.getBody().getStatus()).isEqualTo(OrderStatus.CANCELLED);

        OrdersEntity orderInDb = ordersRepository.findById(orderId).orElseThrow();
        assertThat(orderInDb.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void givenAwaitingOperatorOrder_whenOwnerCancelsOrder_thenStatusIsChangedToCancelled() {
        String clientToken = registerAndLogin();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator_cancel2", "52.2200, 21.0100", 20, service);

        OrderRequest orderRequest = OrderRequest.builder()
                .title("Awaiting Operator Order")
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

        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(newMatchedOrdersRepository.findByOrderIdAndOperatorId(orderId, operator.getId()))
                        .isPresent()
        );

        LoginRequest operatorLogin = LoginRequest.builder()
                .email("operator_cancel2@op.pl")
                .password("pass")
                .build();
        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login", operatorLogin, LoginResponse.class);
        Assertions.assertNotNull(loginResponse.getBody());
        String operatorToken = loginResponse.getBody().token();

        HttpEntity<Void> acceptEntity = new HttpEntity<>(getHeaders(operatorToken));
        testRestTemplate.exchange(
                "/api/orders/acceptOrder/" + orderId,
                HttpMethod.PATCH,
                acceptEntity,
                OrderResponse.class
        );

        HttpEntity<Void> cancelEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<OrderResponse> cancelResponse = testRestTemplate.exchange(
                "/api/orders/cancelOrder/" + orderId,
                HttpMethod.PATCH,
                cancelEntity,
                OrderResponse.class
        );

        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertNotNull(cancelResponse.getBody());
        assertThat(cancelResponse.getBody().getStatus()).isEqualTo(OrderStatus.CANCELLED);

        OrdersEntity orderInDb = ordersRepository.findById(orderId).orElseThrow();
        assertThat(orderInDb.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }


    @Test
    void givenOrdersWithOpenStatus_whenGetOrdersByOpenStatus_thenReturnsOnlyOpenOrders() {
        String clientToken = registerAndLogin();

        // Tworzymy 3 zamówienia OPEN
        for (int i = 0; i < 3; i++) {
            OrderRequest orderRequest = OrderRequest.builder()
                    .title("Open Order " + i)
                    .description("Test description")
                    .service(SERVICE_NAME)
                    .coordinates("52.23, 21.01")
                    .fromDate(LocalDateTime.now().plusDays(1))
                    .toDate(LocalDateTime.now().plusDays(2))
                    .build();

            HttpEntity<OrderRequest> createEntity = new HttpEntity<>(orderRequest, getHeaders(clientToken));
            testRestTemplate.exchange(
                    "/api/orders/createOrder",
                    HttpMethod.POST,
                    createEntity,
                    OrderResponse.class
            );
        }

        // Pobieramy zamówienia ze statusem OPEN
        HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<List<OrderResponse>> getResponse = testRestTemplate.exchange(
                "/api/orders/getOrders/OPEN",
                HttpMethod.GET,
                getEntity,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void givenOrdersWithDifferentStatuses_whenGetOrdersByStatus_thenReturnsOnlyMatchingStatus() {
        String clientToken = registerAndLogin();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        createTestOperator("operator_getorders1", "52.2200, 21.0100", 20, service);

        // Tworzymy zamówienie 1 - pozostanie OPEN
        OrderRequest orderRequest1 = OrderRequest.builder()
                .title("Order 1 - OPEN")
                .description("Test")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        HttpEntity<OrderRequest> createEntity1 = new HttpEntity<>(orderRequest1, getHeaders(clientToken));
        ResponseEntity<OrderResponse> createResponse1 = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                createEntity1,
                OrderResponse.class
        );
        Assertions.assertNotNull(createResponse1.getBody());
        UUID orderId1 = createResponse1.getBody().getId();

        // Tworzymy zamówienie 2 - zmienimy na CANCELLED
        OrderRequest orderRequest2 = OrderRequest.builder()
                .title("Order 2 - CANCELLED")
                .description("Test")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(3))
                .toDate(LocalDateTime.now().plusDays(4))
                .build();

        HttpEntity<OrderRequest> createEntity2 = new HttpEntity<>(orderRequest2, getHeaders(clientToken));
        ResponseEntity<OrderResponse> createResponse2 = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                createEntity2,
                OrderResponse.class
        );
        Assertions.assertNotNull(createResponse2.getBody());
        UUID orderId2 = createResponse2.getBody().getId();

        // Anulujemy zamówienie 2
        HttpEntity<Void> cancelEntity = new HttpEntity<>(getHeaders(clientToken));
        testRestTemplate.exchange(
                "/api/orders/cancelOrder/" + orderId2,
                HttpMethod.PATCH,
                cancelEntity,
                OrderResponse.class
        );

        // Tworzymy zamówienie 3 - zmienimy na COMPLETED
        OrderRequest orderRequest3 = OrderRequest.builder()
                .title("Order 3 - COMPLETED")
                .description("Test")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(5))
                .toDate(LocalDateTime.now().plusDays(6))
                .build();

        HttpEntity<OrderRequest> createEntity3 = new HttpEntity<>(orderRequest3, getHeaders(clientToken));
        ResponseEntity<OrderResponse> createResponse3 = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                createEntity3,
                OrderResponse.class
        );
        Assertions.assertNotNull(createResponse3.getBody());
        UUID orderId3 = createResponse3.getBody().getId();

        // Ręcznie ustawiamy status na COMPLETED
        OrdersEntity order3 = ordersRepository.findById(orderId3).orElseThrow();
        order3.setStatus(OrderStatus.COMPLETED);
        ordersRepository.save(order3);

        // Testujemy GET dla każdego statusu
        HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders(clientToken));

        // GET OPEN
        ResponseEntity<List<OrderResponse>> openResponse = testRestTemplate.exchange(
                "/api/orders/getOrders/OPEN",
                HttpMethod.GET,
                getEntity,
                new ParameterizedTypeReference<>() {
                }
        );
        assertThat(openResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(openResponse.getBody()).isNotNull();
        // Sprawdzamy czy order1 jest w liście
        boolean containsOrder1 = openResponse.getBody().stream()
                .anyMatch(o -> o.getId().equals(orderId1));
        assertThat(containsOrder1).isTrue();

        // GET CANCELLED
        ResponseEntity<List<OrderResponse>> cancelledResponse = testRestTemplate.exchange(
                "/api/orders/getOrders/CANCELLED",
                HttpMethod.GET,
                getEntity,
                new ParameterizedTypeReference<>() {
                }
        );
        assertThat(cancelledResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelledResponse.getBody()).isNotNull();
        boolean containsOrder2 = cancelledResponse.getBody().stream()
                .anyMatch(o -> o.getId().equals(orderId2));
        assertThat(containsOrder2).isTrue();

        // GET COMPLETED
        ResponseEntity<List<OrderResponse>> completedResponse = testRestTemplate.exchange(
                "/api/orders/getOrders/COMPLETED",
                HttpMethod.GET,
                getEntity,
                new ParameterizedTypeReference<>() {
                }
        );
        assertThat(completedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(completedResponse.getBody()).isNotNull();
        boolean containsOrder3 = completedResponse.getBody().stream()
                .anyMatch(o -> o.getId().equals(orderId3));
        assertThat(containsOrder3).isTrue();
    }

    @Test
    void givenNoOrdersWithStatus_whenGetOrdersByStatus_thenReturnsEmptyList() {
        String clientToken = registerAndLogin();

        HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<List<OrderResponse>> getResponse = testRestTemplate.exchange(
                "/api/orders/getOrders/COMPLETED",
                HttpMethod.GET,
                getEntity,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();

        assertThat(getResponse.getBody()).isInstanceOf(List.class);
    }

    @Test
    void givenInvalidStatus_whenGetOrdersByStatus_thenReturnsError() {
        String clientToken = registerAndLogin();

        HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<String> getResponse = testRestTemplate.exchange(
                "/api/orders/getOrders/INVALID_STATUS",
                HttpMethod.GET,
                getEntity,
                String.class
        );

        assertThat(getResponse.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void givenLowercaseStatus_whenGetOrdersByStatus_thenReturnsOrders() {
        String clientToken = registerAndLogin();

        OrderRequest orderRequest = OrderRequest.builder()
                .title("Test Order")
                .description("Test description")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        HttpEntity<OrderRequest> createEntity = new HttpEntity<>(orderRequest, getHeaders(clientToken));
        testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                createEntity,
                OrderResponse.class
        );

        // Pobieramy zamówienia ze statusem w lowercase
        HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders(clientToken));
        var getResponse = testRestTemplate.exchange(
                "/api/orders/getOrders/open",
                HttpMethod.GET,
                getEntity,
                new ParameterizedTypeReference<List<OrderResponse>>() {
                }
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void givenAwaitingOperatorOrders_whenGetOrdersByAwaitingOperatorStatus_thenReturnsMatchingOrders() {
        String clientToken = registerAndLogin();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator_getorders2", "52.2200, 21.0100", 20, service);

        OrderRequest orderRequest = OrderRequest.builder()
                .title("Awaiting Operator Order")
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

        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(newMatchedOrdersRepository.findByOrderIdAndOperatorId(orderId, operator.getId()))
                        .isPresent()
        );

        LoginRequest operatorLogin = LoginRequest.builder()
                .email("operator_getorders2@op.pl")
                .password("pass")
                .build();
        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login", operatorLogin, LoginResponse.class);
        Assertions.assertNotNull(loginResponse.getBody());
        String operatorToken = loginResponse.getBody().token();

        HttpEntity<Void> acceptEntity = new HttpEntity<>(getHeaders(operatorToken));
        testRestTemplate.exchange(
                "/api/orders/acceptOrder/" + orderId,
                HttpMethod.PATCH,
                acceptEntity,
                OrderResponse.class
        );

        HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<List<OrderResponse>> getResponse = testRestTemplate.exchange(
                "/api/orders/getOrders/AWAITING_OPERATOR",
                HttpMethod.GET,
                getEntity,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        boolean containsOrder = getResponse.getBody().stream()
                .anyMatch(o -> o.getId().equals(orderId));
        assertThat(containsOrder).isTrue();
    }

    @Test
    void givenInProgressOrders_whenGetOrdersByInProgressStatus_thenReturnsMatchingOrders() {
        String clientToken = registerAndLogin();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator_getorders3", "52.2200, 21.0100", 20, service);

        OrderRequest orderRequest = OrderRequest.builder()
                .title("In Progress Order")
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

        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(newMatchedOrdersRepository.findByOrderIdAndOperatorId(orderId, operator.getId()))
                        .isPresent()
        );

        LoginRequest operatorLogin = LoginRequest.builder()
                .email("operator_getorders3@op.pl")
                .password("pass")
                .build();
        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login", operatorLogin, LoginResponse.class);
        Assertions.assertNotNull(loginResponse.getBody());
        String operatorToken = loginResponse.getBody().token();

        HttpEntity<Void> acceptEntity = new HttpEntity<>(getHeaders(operatorToken));
        testRestTemplate.exchange(
                "/api/orders/acceptOrder/" + orderId,
                HttpMethod.PATCH,
                acceptEntity,
                OrderResponse.class
        );

        HttpEntity<Void> clientAcceptEntity = new HttpEntity<>(getHeaders(clientToken));
        testRestTemplate.exchange(
                "/api/orders/acceptOrder/" + orderId + "?operatorId=" + operator.getId(),
                HttpMethod.PATCH,
                clientAcceptEntity,
                OrderResponse.class
        );

        HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<List<OrderResponse>> getResponse = testRestTemplate.exchange(
                "/api/orders/getOrders/IN_PROGRESS",
                HttpMethod.GET,
                getEntity,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        boolean containsOrder = getResponse.getBody().stream()
                .anyMatch(o -> o.getId().equals(orderId));
        assertThat(containsOrder).isTrue();
    }

    @Test
    void givenUserWithOrders_whenGetMyOrders_thenReturnsAllUserOrdersSortedByCreatedAtDesc() {
        String clientToken = registerAndLogin();

        // Tworzymy 3 zamówienia dla tego samego użytkownika
        UUID firstOrderId = null;
        UUID secondOrderId = null;
        UUID thirdOrderId = null;

        for (int i = 0; i < 3; i++) {
            OrderRequest orderRequest = OrderRequest.builder()
                    .title("My Order " + i)
                    .description("Description " + i)
                    .service(SERVICE_NAME)
                    .coordinates("52.23, 21.01")
                    .fromDate(LocalDateTime.now().plusDays(1 + i))
                    .toDate(LocalDateTime.now().plusDays(2 + i))
                    .build();

            HttpEntity<OrderRequest> createEntity = new HttpEntity<>(orderRequest, getHeaders(clientToken));
            ResponseEntity<OrderResponse> createResponse = testRestTemplate.exchange(
                    "/api/orders/createOrder",
                    HttpMethod.POST,
                    createEntity,
                    OrderResponse.class
            );

            if (i == 0) {
                Assertions.assertNotNull(createResponse.getBody());
                firstOrderId = createResponse.getBody().getId();
            }
            if (i == 1) {
                Assertions.assertNotNull(createResponse.getBody());
                secondOrderId = createResponse.getBody().getId();
            }
            if (i == 2) {
                Assertions.assertNotNull(createResponse.getBody());
                thirdOrderId = createResponse.getBody().getId();
            }
        }

        // Pobieramy wszystkie zamówienia użytkownika
        HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<List<OrderResponse>> getResponse = testRestTemplate.exchange(
                "/api/orders/getMyOrders",
                HttpMethod.GET,
                getEntity,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody()).hasSize(3);

        // Weryfikacja sortowania - najnowsze powinno być pierwsze (DESC)
        List<OrderResponse> orders = getResponse.getBody();
        assertThat(orders.get(0).getId()).isEqualTo(thirdOrderId);
        assertThat(orders.get(1).getId()).isEqualTo(secondOrderId);
        assertThat(orders.get(2).getId()).isEqualTo(firstOrderId);

        // Weryfikacja że wszystkie zamówienia należą do użytkownika
        orders.forEach(order -> {
            assertThat(order.getTitle()).startsWith("My Order");
            assertThat(order.getService()).isEqualTo(SERVICE_NAME);
        });
    }

    @Test
    void givenUserWithNoOrders_whenGetMyOrders_thenReturnsEmptyList() {
        String clientToken = registerAndLogin();

        // Pobieramy zamówienia bez tworzenia żadnych
        HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<List<OrderResponse>> getResponse = testRestTemplate.exchange(
                "/api/orders/getMyOrders",
                HttpMethod.GET,
                getEntity,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody()).isEmpty();
    }

    @Test
    void givenMultipleUsers_whenGetMyOrders_thenReturnsOnlyCurrentUserOrders() {
        // Pierwszy użytkownik
        String client1Token = registerAndLogin();

        OrderRequest order1 = OrderRequest.builder()
                .title("Client 1 Order")
                .description("First user order")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        HttpEntity<OrderRequest> createEntity1 = new HttpEntity<>(order1, getHeaders(client1Token));
        testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                createEntity1,
                OrderResponse.class
        );

        // Drugi użytkownik
        RegisterRequest user2Register = RegisterRequest.builder()
                .displayName("testClient2")
                .password("password456")
                .name("Anna")
                .surname("Testowa")
                .email("test2@example.com")
                .phoneNumber("987654321")
                .build();

        testRestTemplate.postForEntity("/api/auth/register", user2Register, Void.class);

        LoginRequest user2Login = LoginRequest.builder()
                .email("test2@example.com")
                .password("password456")
                .build();

        ResponseEntity<LoginResponse> login2Response = testRestTemplate.postForEntity(
                "/api/auth/login", user2Login, LoginResponse.class);
        Assertions.assertNotNull(login2Response.getBody());
        String client2Token = login2Response.getBody().token();

        OrderRequest order2 = OrderRequest.builder()
                .title("Client 2 Order")
                .description("Second user order")
                .service(SERVICE_NAME)
                .coordinates("52.24, 21.02")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        HttpEntity<OrderRequest> createEntity2 = new HttpEntity<>(order2, getHeaders(client2Token));
        testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                createEntity2,
                OrderResponse.class
        );

        // Pobieramy zamówienia pierwszego użytkownika
        HttpEntity<Void> getEntity1 = new HttpEntity<>(getHeaders(client1Token));
        ResponseEntity<List<OrderResponse>> getResponse1 = testRestTemplate.exchange(
                "/api/orders/getMyOrders",
                HttpMethod.GET,
                getEntity1,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(getResponse1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse1.getBody()).hasSize(1);
        assertThat(getResponse1.getBody().getFirst().getTitle()).isEqualTo("Client 1 Order");

        // Pobieramy zamówienia drugiego użytkownika
        HttpEntity<Void> getEntity2 = new HttpEntity<>(getHeaders(client2Token));
        ResponseEntity<List<OrderResponse>> getResponse2 = testRestTemplate.exchange(
                "/api/orders/getMyOrders",
                HttpMethod.GET,
                getEntity2,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(getResponse2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse2.getBody()).hasSize(1);
        assertThat(getResponse2.getBody().getFirst().getTitle()).isEqualTo("Client 2 Order");
    }

    @Test
    void givenUserWithOrdersInDifferentStatuses_whenGetMyOrders_thenReturnsAllStatuses() {
        String clientToken = registerAndLogin();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        createTestOperator("operator_myorders", "52.2200, 21.0100", 20, service);

        // Tworzymy zamówienie 1 - OPEN
        OrderRequest orderRequest1 = OrderRequest.builder()
                .title("Order OPEN")
                .description("Test")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        HttpEntity<OrderRequest> createEntity1 = new HttpEntity<>(orderRequest1, getHeaders(clientToken));
        ResponseEntity<OrderResponse> createResponse1 = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                createEntity1,
                OrderResponse.class
        );
        Assertions.assertNotNull(createResponse1.getBody());

        // Tworzymy zamówienie 2 - będzie AWAITING_OPERATOR
        OrderRequest orderRequest2 = OrderRequest.builder()
                .title("Order AWAITING")
                .description("Test")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(3))
                .toDate(LocalDateTime.now().plusDays(4))
                .build();

        HttpEntity<OrderRequest> createEntity2 = new HttpEntity<>(orderRequest2, getHeaders(clientToken));
        ResponseEntity<OrderResponse> createResponse2 = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                createEntity2,
                OrderResponse.class
        );
        Assertions.assertNotNull(createResponse2.getBody());
        UUID order2Id = createResponse2.getBody().getId();

        // Czekamy na dopasowanie
        await().atMost(5, SECONDS).untilAsserted(() -> {
            List<NewMatchedOrderEntity> matches = newMatchedOrdersRepository
                    .findAll().stream()
                    .filter(m -> m.getOrder().getId().equals(order2Id))
                    .toList();
            assertThat(matches).hasSize(1);
        });

        // Operator akceptuje zamówienie 2
        LoginRequest operatorLogin = LoginRequest.builder()
                .email("operator_myorders@op.pl")
                .password("pass")
                .build();
        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login", operatorLogin, LoginResponse.class);
        Assertions.assertNotNull(loginResponse.getBody());
        String operatorToken = loginResponse.getBody().token();

        HttpEntity<Void> acceptEntity = new HttpEntity<>(getHeaders(operatorToken));
        testRestTemplate.exchange(
                "/api/orders/acceptOrder/" + order2Id,
                HttpMethod.PATCH,
                acceptEntity,
                OrderResponse.class
        );

        // Tworzymy zamówienie 3 - będzie CANCELLED
        OrderRequest orderRequest3 = OrderRequest.builder()
                .title("Order CANCELLED")
                .description("Test")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(5))
                .toDate(LocalDateTime.now().plusDays(6))
                .build();

        HttpEntity<OrderRequest> createEntity3 = new HttpEntity<>(orderRequest3, getHeaders(clientToken));
        ResponseEntity<OrderResponse> createResponse3 = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                createEntity3,
                OrderResponse.class
        );
        Assertions.assertNotNull(createResponse3.getBody());
        UUID order3Id = createResponse3.getBody().getId();

        // Anulujemy zamówienie 3
        HttpEntity<Void> cancelEntity = new HttpEntity<>(getHeaders(clientToken));
        testRestTemplate.exchange(
                "/api/orders/cancelOrder/" + order3Id,
                HttpMethod.PATCH,
                cancelEntity,
                OrderResponse.class
        );

        // Pobieramy wszystkie zamówienia użytkownika
        HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<List<OrderResponse>> getResponse = testRestTemplate.exchange(
                "/api/orders/getMyOrders",
                HttpMethod.GET,
                getEntity,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody()).hasSize(3);

        // Weryfikacja że zwrócone są wszystkie statusy
        List<OrderStatus> statuses = getResponse.getBody().stream()
                .map(OrderResponse::getStatus)
                .toList();

        assertThat(statuses).contains(OrderStatus.OPEN);
        assertThat(statuses).contains(OrderStatus.AWAITING_OPERATOR);
        assertThat(statuses).contains(OrderStatus.CANCELLED);
    }

    @Test
    void givenUnauthenticatedUser_whenGetMyOrders_thenReturnsUnauthorized() {
        // Próba pobrania zamówień bez tokenu
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> getEntity = new HttpEntity<>(headers);

        ResponseEntity<String> getResponse = testRestTemplate.exchange(
                "/api/orders/getMyOrders",
                HttpMethod.GET,
                getEntity,
                String.class
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void givenOrdersCreatedAtDifferentTimes_whenGetMyOrders_thenReturnsSortedByCreatedAtDesc() throws InterruptedException {
        String clientToken = registerAndLogin();

        // Tworzymy pierwsze zamówienie
        OrderRequest orderRequest1 = OrderRequest.builder()
                .title("First Order Time Test")
                .description("Created first")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        HttpEntity<OrderRequest> createEntity1 = new HttpEntity<>(orderRequest1, getHeaders(clientToken));
        ResponseEntity<OrderResponse> createResponse1 = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                createEntity1,
                OrderResponse.class
        );
        Assertions.assertNotNull(createResponse1.getBody());
        UUID firstOrderId = createResponse1.getBody().getId();

        // Czekamy, aby upewnić się, że timestamp będzie inny
        Thread.sleep(1000);

        // Tworzymy drugie zamówienie
        OrderRequest orderRequest2 = OrderRequest.builder()
                .title("Second Order Time Test")
                .description("Created second")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(3))
                .toDate(LocalDateTime.now().plusDays(4))
                .build();

        HttpEntity<OrderRequest> createEntity2 = new HttpEntity<>(orderRequest2, getHeaders(clientToken));
        ResponseEntity<OrderResponse> createResponse2 = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                createEntity2,
                OrderResponse.class
        );
        Assertions.assertNotNull(createResponse2.getBody());
        UUID secondOrderId = createResponse2.getBody().getId();

        // Pobieramy zamówienia
        HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<List<OrderResponse>> getResponse = testRestTemplate.exchange(
                "/api/orders/getMyOrders",
                HttpMethod.GET,
                getEntity,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();

        // Filtrujemy tylko zamówienia z tego testu (aby uniknąć konfliktów z innymi testami)
        List<OrderResponse> orders = getResponse.getBody().stream()
                .filter(o -> o.getTitle().contains("Time Test"))
                .toList();

        assertThat(orders).hasSize(2);

        // Najnowsze zamówienie (drugie) powinno być na pierwszym miejscu
        assertThat(orders.get(0).getId()).isEqualTo(secondOrderId);
        assertThat(orders.get(0).getTitle()).isEqualTo("Second Order Time Test");

        // Starsze zamówienie (pierwsze) powinno być na drugim miejscu
        assertThat(orders.get(1).getId()).isEqualTo(firstOrderId);
        assertThat(orders.get(1).getTitle()).isEqualTo("First Order Time Test");

        // Weryfikacja sortowania DESC - drugie zamówienie powinno być utworzone po pierwszym
        assertThat(orders.get(0).getCreatedAt()).isAfter(orders.get(1).getCreatedAt());
    }
}