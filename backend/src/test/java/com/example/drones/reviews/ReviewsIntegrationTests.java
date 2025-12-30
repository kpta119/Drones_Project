package com.example.drones.reviews;

import com.example.drones.auth.dto.LoginRequest;
import com.example.drones.auth.dto.LoginResponse;
import com.example.drones.auth.dto.RegisterRequest;
import com.example.drones.orders.OrderStatus;
import com.example.drones.orders.OrdersEntity;
import com.example.drones.orders.OrdersRepository;
import com.example.drones.reviews.dto.ReviewRequest;
import com.example.drones.reviews.dto.ReviewResponse;
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

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ReviewsIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ReviewsRepository reviewsRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServicesRepository servicesRepository;

    @Autowired
    private OperatorServicesRepository operatorServicesRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private RegisterRequest clientRegister;
    private LoginRequest clientLogin;
    private final String SERVICE_NAME = "Laser Scanning";

    @BeforeEach
    void setUp() {
        if (!servicesRepository.existsById(SERVICE_NAME)) {
            ServicesEntity service = new ServicesEntity();
            service.setName(SERVICE_NAME);
            servicesRepository.save(service);
        }

        clientRegister = RegisterRequest.builder()
                .displayName("testClient")
                .password("password123")
                .name("Jan")
                .surname("Testowy")
                .email("client@example.com")
                .phoneNumber("123456789")
                .build();

        clientLogin = LoginRequest.builder()
                .email("client@example.com")
                .password("password123")
                .build();
    }

    @AfterEach
    void tearDown() {
        reviewsRepository.deleteAll();
        ordersRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerAndLoginClient() {
        testRestTemplate.postForEntity("/api/auth/register", clientRegister, Void.class);
        ResponseEntity<LoginResponse> response = testRestTemplate.postForEntity("/api/auth/login", clientLogin, LoginResponse.class);
        Assertions.assertNotNull(response.getBody());
        return response.getBody().token();
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

    private String loginAsOperator(String email) {
        LoginRequest operatorLogin = LoginRequest.builder()
                .email(email)
                .password("pass")
                .build();
        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login", operatorLogin, LoginResponse.class);
        Assertions.assertNotNull(loginResponse.getBody());
        return loginResponse.getBody().token();
    }

    private HttpHeaders getHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-TOKEN", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private OrdersEntity createCompletedOrder(UserEntity client, UserEntity operator) {
        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();

        OrdersEntity order = OrdersEntity.builder()
                .title("Test Order")
                .description("Test description")
                .user(client)
                .service(service)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .status(OrderStatus.COMPLETED)
                .build();

        return ordersRepository.save(order);
    }

    private OrdersEntity createInProgressOrder(UserEntity client, UserEntity operator) {
        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();

        OrdersEntity order = OrdersEntity.builder()
                .title("Test Order In Progress")
                .description("Test description")
                .user(client)
                .service(service)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .status(OrderStatus.IN_PROGRESS)
                .build();

        return ordersRepository.save(order);
    }

    private OrdersEntity createOpenOrder(UserEntity client) {
        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();

        OrdersEntity order = OrdersEntity.builder()
                .title("Test Order Open")
                .description("Test description")
                .user(client)
                .service(service)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .status(OrderStatus.OPEN)
                .build();

        return ordersRepository.save(order);
    }

    @Test
    void givenCompletedOrder_whenClientCreatesReviewForOperator_thenReviewIsCreated() {
        String clientToken = registerAndLoginClient();
        UserEntity client = userRepository.findByEmail("client@example.com").orElseThrow();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator1", "52.2200, 21.0100", 20, service);

        OrdersEntity order = createCompletedOrder(client, operator);

        // When: Klient tworzy recenzję dla operatora
        ReviewRequest reviewRequest = ReviewRequest.builder()
                .stars(5)
                .body("Excellent work, very professional!")
                .build();

        HttpEntity<ReviewRequest> requestEntity = new HttpEntity<>(reviewRequest, getHeaders(clientToken));
        ResponseEntity<ReviewResponse> response = testRestTemplate.exchange(
                "/api/reviews/createReview/" + order.getId() + "/" + operator.getId(),
                HttpMethod.POST,
                requestEntity,
                ReviewResponse.class
        );

        // Then: Recenzja została utworzona
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStars()).isEqualTo(5);
        assertThat(response.getBody().getBody()).isEqualTo("Excellent work, very professional!");
        assertThat(response.getBody().getAuthorId()).isEqualTo(client.getId());
        assertThat(response.getBody().getTargetId()).isEqualTo(operator.getId());

        // Weryfikacja w bazie danych
        List<ReviewEntity> reviews = reviewsRepository.findAll();
        assertThat(reviews).hasSize(1);
        ReviewEntity savedReview = reviews.get(0);
        assertThat(savedReview.getStars()).isEqualTo(5);
        assertThat(savedReview.getAuthor().getId()).isEqualTo(client.getId());
        assertThat(savedReview.getTarget().getId()).isEqualTo(operator.getId());
        assertThat(savedReview.getOrder().getId()).isEqualTo(order.getId());
    }

    @Test
    void givenCompletedOrder_whenOperatorCreatesReviewForClient_thenReviewIsCreated() {
        // Given: Zarejestrowany klient i operator z ukończonym zamówieniem
        String clientToken = registerAndLoginClient();
        UserEntity client = userRepository.findByEmail("client@example.com").orElseThrow();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator2", "52.2200, 21.0100", 20, service);

        OrdersEntity order = createCompletedOrder(client, operator);

        String operatorToken = loginAsOperator("operator2@op.pl");

        // When: Operator tworzy recenzję dla klienta
        ReviewRequest reviewRequest = ReviewRequest.builder()
                .stars(4)
                .body("Great client, clear instructions.")
                .build();

        HttpEntity<ReviewRequest> requestEntity = new HttpEntity<>(reviewRequest, getHeaders(operatorToken));
        ResponseEntity<ReviewResponse> response = testRestTemplate.exchange(
                "/api/reviews/createReview/" + order.getId() + "/" + client.getId(),
                HttpMethod.POST,
                requestEntity,
                ReviewResponse.class
        );

        // Then: Recenzja została utworzona
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStars()).isEqualTo(4);
        assertThat(response.getBody().getBody()).isEqualTo("Great client, clear instructions.");
        assertThat(response.getBody().getAuthorId()).isEqualTo(operator.getId());
        assertThat(response.getBody().getTargetId()).isEqualTo(client.getId());

        // Weryfikacja w bazie danych
        List<ReviewEntity> reviews = reviewsRepository.findAll();
        assertThat(reviews).hasSize(1);
        ReviewEntity savedReview = reviews.get(0);
        assertThat(savedReview.getStars()).isEqualTo(4);
        assertThat(savedReview.getAuthor().getId()).isEqualTo(operator.getId());
        assertThat(savedReview.getTarget().getId()).isEqualTo(client.getId());
    }

    @Test
    void givenInProgressOrder_whenClientCreatesReview_thenReviewIsCreated() {
        // Given: Zamówienie w trakcie realizacji
        String clientToken = registerAndLoginClient();
        UserEntity client = userRepository.findByEmail("client@example.com").orElseThrow();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator3", "52.2200, 21.0100", 20, service);

        OrdersEntity order = createInProgressOrder(client, operator);

        // When: Klient tworzy recenzję dla zamówienia w trakcie
        ReviewRequest reviewRequest = ReviewRequest.builder()
                .stars(5)
                .body("Great service so far!")
                .build();

        HttpEntity<ReviewRequest> requestEntity = new HttpEntity<>(reviewRequest, getHeaders(clientToken));
        ResponseEntity<ReviewResponse> response = testRestTemplate.exchange(
                "/api/reviews/createReview/" + order.getId() + "/" + operator.getId(),
                HttpMethod.POST,
                requestEntity,
                ReviewResponse.class
        );

        // Then: Recenzja została utworzona (IN_PROGRESS jest dozwolone)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStars()).isEqualTo(5);
    }

    @Test
    void givenOpenOrder_whenClientCreatesReview_thenReturnsBadRequest() {
        // Given: Zamówienie w statusie OPEN (bez operatora)
        String clientToken = registerAndLoginClient();
        UserEntity client = userRepository.findByEmail("client@example.com").orElseThrow();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator4", "52.2200, 21.0100", 20, service);

        OrdersEntity order = createOpenOrder(client);

        // When: Klient próbuje utworzyć recenzję dla zamówienia OPEN
        ReviewRequest reviewRequest = ReviewRequest.builder()
                .stars(5)
                .body("Trying to review open order")
                .build();

        HttpEntity<ReviewRequest> requestEntity = new HttpEntity<>(reviewRequest, getHeaders(clientToken));
        ResponseEntity<String> response = testRestTemplate.exchange(
                "/api/reviews/createReview/" + order.getId() + "/" + operator.getId(),
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // Then: Zwrócony błąd - zamówienie nie może być recenzowane
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();

        // Weryfikacja że recenzja nie została utworzona
        List<ReviewEntity> reviews = reviewsRepository.findAll();
        assertThat(reviews).isEmpty();
    }

    @Test
    void givenCompletedOrder_whenUserTriesToReviewThemselves_thenReturnsBadRequest() {
        // Given: Ukończone zamówienie
        String clientToken = registerAndLoginClient();
        UserEntity client = userRepository.findByEmail("client@example.com").orElseThrow();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator5", "52.2200, 21.0100", 20, service);

        OrdersEntity order = createCompletedOrder(client, operator);

        // When: Klient próbuje utworzyć recenzję dla samego siebie
        ReviewRequest reviewRequest = ReviewRequest.builder()
                .stars(5)
                .body("I am great!")
                .build();

        HttpEntity<ReviewRequest> requestEntity = new HttpEntity<>(reviewRequest, getHeaders(clientToken));
        ResponseEntity<String> response = testRestTemplate.exchange(
                "/api/reviews/createReview/" + order.getId() + "/" + client.getId(),
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // Then: Zwrócony błąd - nie można recenzować samego siebie
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();

        // Weryfikacja że recenzja nie została utworzona
        List<ReviewEntity> reviews = reviewsRepository.findAll();
        assertThat(reviews).isEmpty();
    }

    @Test
    void givenExistingReview_whenUserTriesToCreateDuplicateReview_thenReturnsBadRequest() {
        // Given: Klient i operator z ukończonym zamówieniem i istniejącą recenzją
        String clientToken = registerAndLoginClient();
        UserEntity client = userRepository.findByEmail("client@example.com").orElseThrow();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator6", "52.2200, 21.0100", 20, service);

        OrdersEntity order = createCompletedOrder(client, operator);

        // Tworzymy pierwszą recenzję
        ReviewRequest reviewRequest1 = ReviewRequest.builder()
                .stars(5)
                .body("First review")
                .build();

        HttpEntity<ReviewRequest> requestEntity1 = new HttpEntity<>(reviewRequest1, getHeaders(clientToken));
        ResponseEntity<ReviewResponse> response1 = testRestTemplate.exchange(
                "/api/reviews/createReview/" + order.getId() + "/" + operator.getId(),
                HttpMethod.POST,
                requestEntity1,
                ReviewResponse.class
        );

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // When: Klient próbuje utworzyć duplikat recenzji
        ReviewRequest reviewRequest2 = ReviewRequest.builder()
                .stars(4)
                .body("Second review - duplicate")
                .build();

        HttpEntity<ReviewRequest> requestEntity2 = new HttpEntity<>(reviewRequest2, getHeaders(clientToken));
        ResponseEntity<String> response2 = testRestTemplate.exchange(
                "/api/reviews/createReview/" + order.getId() + "/" + operator.getId(),
                HttpMethod.POST,
                requestEntity2,
                String.class
        );

        // Then: Zwrócony błąd - recenzja już istnieje
        assertThat(response2.getStatusCode().is4xxClientError()).isTrue();

        // Weryfikacja że jest tylko jedna recenzja
        List<ReviewEntity> reviews = reviewsRepository.findAll();
        assertThat(reviews).hasSize(1);
        assertThat(reviews.get(0).getBody()).isEqualTo("First review");
    }

    @Test
    void givenNonExistentOrder_whenUserCreatesReview_thenReturnsNotFound() {
        // Given: Zarejestrowany klient
        String clientToken = registerAndLoginClient();
        UserEntity client = userRepository.findByEmail("client@example.com").orElseThrow();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator7", "52.2200, 21.0100", 20, service);

        UUID nonExistentOrderId = UUID.randomUUID();

        // When: Klient próbuje utworzyć recenzję dla nieistniejącego zamówienia
        ReviewRequest reviewRequest = ReviewRequest.builder()
                .stars(5)
                .body("Review for non-existent order")
                .build();

        HttpEntity<ReviewRequest> requestEntity = new HttpEntity<>(reviewRequest, getHeaders(clientToken));
        ResponseEntity<String> response = testRestTemplate.exchange(
                "/api/reviews/createReview/" + nonExistentOrderId + "/" + operator.getId(),
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // Then: Zwrócony błąd - zamówienie nie istnieje
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();

        // Weryfikacja że recenzja nie została utworzona
        List<ReviewEntity> reviews = reviewsRepository.findAll();
        assertThat(reviews).isEmpty();
    }

    @Test
    void givenNonExistentTarget_whenUserCreatesReview_thenReturnsNotFound() {
        // Given: Klient z ukończonym zamówieniem
        String clientToken = registerAndLoginClient();
        UserEntity client = userRepository.findByEmail("client@example.com").orElseThrow();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator8", "52.2200, 21.0100", 20, service);

        OrdersEntity order = createCompletedOrder(client, operator);

        UUID nonExistentUserId = UUID.randomUUID();

        // When: Klient próbuje utworzyć recenzję dla nieistniejącego użytkownika
        ReviewRequest reviewRequest = ReviewRequest.builder()
                .stars(5)
                .body("Review for non-existent user")
                .build();

        HttpEntity<ReviewRequest> requestEntity = new HttpEntity<>(reviewRequest, getHeaders(clientToken));
        ResponseEntity<String> response = testRestTemplate.exchange(
                "/api/reviews/createReview/" + order.getId() + "/" + nonExistentUserId,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // Then: Zwrócony błąd - użytkownik nie istnieje
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();

        // Weryfikacja że recenzja nie została utworzona
        List<ReviewEntity> reviews = reviewsRepository.findAll();
        assertThat(reviews).isEmpty();
    }

    @Test
    void givenValidReview_whenStarsIsOutOfRange_thenReturnsBadRequest() {
        // Given: Klient i operator z ukończonym zamówieniem
        String clientToken = registerAndLoginClient();
        UserEntity client = userRepository.findByEmail("client@example.com").orElseThrow();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator9", "52.2200, 21.0100", 20, service);

        OrdersEntity order = createCompletedOrder(client, operator);

        // When: Klient próbuje utworzyć recenzję z nieprawidłową oceną (0 lub 6)
        ReviewRequest reviewRequest = ReviewRequest.builder()
                .stars(0)  // Nieprawidłowa ocena (min to 1)
                .body("Invalid stars")
                .build();

        HttpEntity<ReviewRequest> requestEntity = new HttpEntity<>(reviewRequest, getHeaders(clientToken));
        ResponseEntity<String> response = testRestTemplate.exchange(
                "/api/reviews/createReview/" + order.getId() + "/" + operator.getId(),
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // Then: Zwrócony błąd walidacji
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();

        // Weryfikacja że recenzja nie została utworzona
        List<ReviewEntity> reviews = reviewsRepository.findAll();
        assertThat(reviews).isEmpty();
    }

    @Test
    void givenUnauthenticatedUser_whenCreatingReview_thenReturnsUnauthorized() {
        // Given: Nieuwierzytelniony użytkownik
        String clientToken = registerAndLoginClient();
        UserEntity client = userRepository.findByEmail("client@example.com").orElseThrow();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator10", "52.2200, 21.0100", 20, service);

        OrdersEntity order = createCompletedOrder(client, operator);

        // When: Próba utworzenia recenzji bez tokenu
        ReviewRequest reviewRequest = ReviewRequest.builder()
                .stars(5)
                .body("Unauthorized review")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ReviewRequest> requestEntity = new HttpEntity<>(reviewRequest, headers);

        ResponseEntity<String> response = testRestTemplate.exchange(
                "/api/reviews/createReview/" + order.getId() + "/" + operator.getId(),
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // Then: Zwrócony błąd autoryzacji
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Weryfikacja że recenzja nie została utworzona
        List<ReviewEntity> reviews = reviewsRepository.findAll();
        assertThat(reviews).isEmpty();
    }

    @Test
    void givenCompletedOrder_whenBothUsersCreateReviews_thenBothReviewsAreCreated() {
        // Given: Klient i operator z ukończonym zamówieniem
        String clientToken = registerAndLoginClient();
        UserEntity client = userRepository.findByEmail("client@example.com").orElseThrow();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator11", "52.2200, 21.0100", 20, service);

        OrdersEntity order = createCompletedOrder(client, operator);

        String operatorToken = loginAsOperator("operator11@op.pl");

        // When: Klient tworzy recenzję dla operatora
        ReviewRequest clientReview = ReviewRequest.builder()
                .stars(5)
                .body("Great operator!")
                .build();

        HttpEntity<ReviewRequest> clientRequest = new HttpEntity<>(clientReview, getHeaders(clientToken));
        ResponseEntity<ReviewResponse> clientResponse = testRestTemplate.exchange(
                "/api/reviews/createReview/" + order.getId() + "/" + operator.getId(),
                HttpMethod.POST,
                clientRequest,
                ReviewResponse.class
        );

        // And: Operator tworzy recenzję dla klienta
        ReviewRequest operatorReview = ReviewRequest.builder()
                .stars(4)
                .body("Good client!")
                .build();

        HttpEntity<ReviewRequest> operatorRequest = new HttpEntity<>(operatorReview, getHeaders(operatorToken));
        ResponseEntity<ReviewResponse> operatorResponse = testRestTemplate.exchange(
                "/api/reviews/createReview/" + order.getId() + "/" + client.getId(),
                HttpMethod.POST,
                operatorRequest,
                ReviewResponse.class
        );

        // Then: Obie recenzje zostały utworzone
        assertThat(clientResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(operatorResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        List<ReviewEntity> reviews = reviewsRepository.findAll();
        assertThat(reviews).hasSize(2);

        // Weryfikacja że jedna recenzja jest od klienta dla operatora
        boolean hasClientReview = reviews.stream()
                .anyMatch(r -> r.getAuthor().getId().equals(client.getId())
                        && r.getTarget().getId().equals(operator.getId()));
        assertThat(hasClientReview).isTrue();

        // Weryfikacja że druga recenzja jest od operatora dla klienta
        boolean hasOperatorReview = reviews.stream()
                .anyMatch(r -> r.getAuthor().getId().equals(operator.getId())
                        && r.getTarget().getId().equals(client.getId()));
        assertThat(hasOperatorReview).isTrue();
    }

    @Test
    void givenValidReview_whenBodyIsEmpty_thenReviewIsCreatedWithoutBody() {
        // Given: Klient i operator z ukończonym zamówieniem
        String clientToken = registerAndLoginClient();
        UserEntity client = userRepository.findByEmail("client@example.com").orElseThrow();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator12", "52.2200, 21.0100", 20, service);

        OrdersEntity order = createCompletedOrder(client, operator);

        // When: Klient tworzy recenzję bez treści (tylko ocena)
        ReviewRequest reviewRequest = ReviewRequest.builder()
                .stars(5)
                .build();

        HttpEntity<ReviewRequest> requestEntity = new HttpEntity<>(reviewRequest, getHeaders(clientToken));
        ResponseEntity<ReviewResponse> response = testRestTemplate.exchange(
                "/api/reviews/createReview/" + order.getId() + "/" + operator.getId(),
                HttpMethod.POST,
                requestEntity,
                ReviewResponse.class
        );

        // Then: Recenzja została utworzona z pustą treścią
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStars()).isEqualTo(5);
        assertThat(response.getBody().getBody()).isNullOrEmpty();

        // Weryfikacja w bazie danych
        List<ReviewEntity> reviews = reviewsRepository.findAll();
        assertThat(reviews).hasSize(1);
    }
}
