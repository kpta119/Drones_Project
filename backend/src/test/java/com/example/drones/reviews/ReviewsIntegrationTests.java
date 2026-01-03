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

    private UserEntity createTestOperator(String username, ServicesEntity service) {
        UserEntity operator = UserEntity.builder()
                .displayName(username)
                .email(username + "@op.pl")
                .password(passwordEncoder.encode("pass"))
                .role(UserRole.OPERATOR)
                .name("Op").surname("Erator")
                .coordinates("52.2200, 21.0100")
                .radius(20)
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

    private OrdersEntity createCompletedOrder(UserEntity client) {
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

    private OrdersEntity createInProgressOrder(UserEntity client) {
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
        UserEntity operator = createTestOperator("operator1", service);

        OrdersEntity order = createCompletedOrder(client);

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
        ReviewEntity savedReview = reviews.getFirst();
        assertThat(savedReview.getStars()).isEqualTo(5);
        assertThat(savedReview.getAuthor().getId()).isEqualTo(client.getId());
        assertThat(savedReview.getTarget().getId()).isEqualTo(operator.getId());
        assertThat(savedReview.getOrder().getId()).isEqualTo(order.getId());
    }

    @Test
    void givenCompletedOrder_whenOperatorCreatesReviewForClient_thenReviewIsCreated() {
        // Given: Zarejestrowany klient i operator z ukończonym zamówieniem
        registerAndLoginClient();
        UserEntity client = userRepository.findByEmail("client@example.com").orElseThrow();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator2", service);

        OrdersEntity order = createCompletedOrder(client);

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
        ReviewEntity savedReview = reviews.getFirst();
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
        UserEntity operator = createTestOperator("operator3", service);

        OrdersEntity order = createInProgressOrder(client);

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
        UserEntity operator = createTestOperator("operator4", service);

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
        createTestOperator("operator5", service);

        OrdersEntity order = createCompletedOrder(client);

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
        UserEntity operator = createTestOperator("operator6", service);

        OrdersEntity order = createCompletedOrder(client);

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
        assertThat(reviews.getFirst().getBody()).isEqualTo("First review");
    }

    @Test
    void givenNonExistentOrder_whenUserCreatesReview_thenReturnsNotFound() {
        // Given: Zarejestrowany klient
        String clientToken = registerAndLoginClient();
        userRepository.findByEmail("client@example.com").orElseThrow();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator7", service);

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
        createTestOperator("operator8", service);

        OrdersEntity order = createCompletedOrder(client);

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
        UserEntity operator = createTestOperator("operator9", service);

        OrdersEntity order = createCompletedOrder(client);

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
        registerAndLoginClient();
        UserEntity client = userRepository.findByEmail("client@example.com").orElseThrow();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator10", service);

        OrdersEntity order = createCompletedOrder(client);

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
        UserEntity operator = createTestOperator("operator11", service);

        OrdersEntity order = createCompletedOrder(client);

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
        UserEntity operator = createTestOperator("operator12", service);

        OrdersEntity order = createCompletedOrder(client);

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


    @Test
    void givenUserWithReviews_whenGetUserReviews_thenReturnsAllReviewsForThatUser() {
        // Given: Klient z dwiema recenzjami od różnych operatorów
        String clientToken = registerAndLoginClient();
        UserEntity client = userRepository.findByEmail("client@example.com").orElseThrow();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        createTestOperator("operator_reviews1", service);
        createTestOperator("operator_reviews2", service);

        OrdersEntity order1 = createCompletedOrder(client);
        OrdersEntity order2 = createCompletedOrder(client);

        // Operator 1 tworzy recenzję dla klienta
        String operator1Token = loginAsOperator("operator_reviews1@op.pl");
        ReviewRequest review1 = ReviewRequest.builder()
                .stars(5)
                .body("Great client to work with!")
                .build();

        HttpEntity<ReviewRequest> request1 = new HttpEntity<>(review1, getHeaders(operator1Token));
        testRestTemplate.exchange(
                "/api/reviews/createReview/" + order1.getId() + "/" + client.getId(),
                HttpMethod.POST,
                request1,
                ReviewResponse.class
        );

        // Operator 2 tworzy recenzję dla klienta
        String operator2Token = loginAsOperator("operator_reviews2@op.pl");
        ReviewRequest review2 = ReviewRequest.builder()
                .stars(4)
                .body("Good communication.")
                .build();

        HttpEntity<ReviewRequest> request2 = new HttpEntity<>(review2, getHeaders(operator2Token));
        testRestTemplate.exchange(
                "/api/reviews/createReview/" + order2.getId() + "/" + client.getId(),
                HttpMethod.POST,
                request2,
                ReviewResponse.class
        );

        // When: Pobieramy recenzje użytkownika
        HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<List<com.example.drones.reviews.dto.UserReviewResponse>> getResponse = testRestTemplate.exchange(
                "/api/reviews/getUserReviews/" + client.getId(),
                HttpMethod.GET,
                getEntity,
                new org.springframework.core.ParameterizedTypeReference<>() {}
        );

        // Then: Zwrócone są obie recenzje
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody()).hasSize(2);

        List<com.example.drones.reviews.dto.UserReviewResponse> reviews = getResponse.getBody();

        // Weryfikacja zawartości recenzji
        boolean hasOperator1Review = reviews.stream()
                .anyMatch(r -> r.getStars() == 5 && r.getBody().equals("Great client to work with!"));
        assertThat(hasOperator1Review).isTrue();

        boolean hasOperator2Review = reviews.stream()
                .anyMatch(r -> r.getStars() == 4 && r.getBody().equals("Good communication."));
        assertThat(hasOperator2Review).isTrue();
    }

    @Test
    void givenUserWithNoReviews_whenGetUserReviews_thenReturnsEmptyList() {
        // Given: Zarejestrowany klient bez recenzji
        String clientToken = registerAndLoginClient();
        UserEntity client = userRepository.findByEmail("client@example.com").orElseThrow();

        // When: Pobieramy recenzje użytkownika bez recenzji
        HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<List<com.example.drones.reviews.dto.UserReviewResponse>> getResponse = testRestTemplate.exchange(
                "/api/reviews/getUserReviews/" + client.getId(),
                HttpMethod.GET,
                getEntity,
                new org.springframework.core.ParameterizedTypeReference<>() {}
        );

        // Then: Zwrócona jest pusta lista
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody()).isEmpty();
    }

    @Test
    void givenNonExistentUser_whenGetUserReviews_thenReturnsNotFound() {
        // Given: Zarejestrowany klient
        String clientToken = registerAndLoginClient();
        UUID nonExistentUserId = UUID.randomUUID();

        // When: Próba pobrania recenzji dla nieistniejącego użytkownika
        HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<String> getResponse = testRestTemplate.exchange(
                "/api/reviews/getUserReviews/" + nonExistentUserId,
                HttpMethod.GET,
                getEntity,
                String.class
        );

        // Then: Zwrócony błąd - użytkownik nie istnieje
        assertThat(getResponse.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void givenOperatorWithReviews_whenGetUserReviews_thenReturnsAllReviewsForOperator() {
        // Given: Operator z recenzjami od różnych klientów
        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator_reviews3", service);

        // Pierwszy klient
        String client1Token = registerAndLoginClient();
        UserEntity client1 = userRepository.findByEmail("client@example.com").orElseThrow();

        OrdersEntity order1 = createCompletedOrder(client1);

        ReviewRequest review1 = ReviewRequest.builder()
                .stars(5)
                .body("Excellent operator!")
                .build();

        HttpEntity<ReviewRequest> request1 = new HttpEntity<>(review1, getHeaders(client1Token));
        testRestTemplate.exchange(
                "/api/reviews/createReview/" + order1.getId() + "/" + operator.getId(),
                HttpMethod.POST,
                request1,
                ReviewResponse.class
        );

        // Drugi klient
        RegisterRequest client2Register = RegisterRequest.builder()
                .displayName("testClient2")
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

        ResponseEntity<LoginResponse> login2Response = testRestTemplate.postForEntity(
                "/api/auth/login", client2Login, LoginResponse.class);
        Assertions.assertNotNull(login2Response.getBody());
        String client2Token = login2Response.getBody().token();

        UserEntity client2 = userRepository.findByEmail("client2@example.com").orElseThrow();

        OrdersEntity order2 = createCompletedOrder(client2);

        ReviewRequest review2 = ReviewRequest.builder()
                .stars(3)
                .body("Average service.")
                .build();

        HttpEntity<ReviewRequest> request2 = new HttpEntity<>(review2, getHeaders(client2Token));
        testRestTemplate.exchange(
                "/api/reviews/createReview/" + order2.getId() + "/" + operator.getId(),
                HttpMethod.POST,
                request2,
                ReviewResponse.class
        );

        // When: Pobieramy recenzje operatora
        HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders(client1Token));
        ResponseEntity<List<com.example.drones.reviews.dto.UserReviewResponse>> getResponse = testRestTemplate.exchange(
                "/api/reviews/getUserReviews/" + operator.getId(),
                HttpMethod.GET,
                getEntity,
                new org.springframework.core.ParameterizedTypeReference<>() {}
        );

        // Then: Zwrócone są obie recenzje
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody()).hasSize(2);

        List<com.example.drones.reviews.dto.UserReviewResponse> reviews = getResponse.getBody();

        // Weryfikacja zawartości
        boolean hasClient1Review = reviews.stream()
                .anyMatch(r -> r.getStars() == 5 && r.getBody().equals("Excellent operator!"));
        assertThat(hasClient1Review).isTrue();

        boolean hasClient2Review = reviews.stream()
                .anyMatch(r -> r.getStars() == 3 && r.getBody().equals("Average service."));
        assertThat(hasClient2Review).isTrue();
    }

    @Test
    void givenUserWithMixedReviews_whenGetUserReviews_thenReturnsOnlyReviewsWhereUserIsTarget() {
        // Given: Klient który ma recenzje jako target i jako author
        String clientToken = registerAndLoginClient();
        UserEntity client = userRepository.findByEmail("client@example.com").orElseThrow();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator_reviews4", service);

        OrdersEntity order = createCompletedOrder(client);

        // Operator tworzy recenzję dla klienta (klient jest target)
        String operatorToken = loginAsOperator("operator_reviews4@op.pl");
        ReviewRequest operatorReview = ReviewRequest.builder()
                .stars(5)
                .body("Great client!")
                .build();

        HttpEntity<ReviewRequest> operatorRequest = new HttpEntity<>(operatorReview, getHeaders(operatorToken));
        testRestTemplate.exchange(
                "/api/reviews/createReview/" + order.getId() + "/" + client.getId(),
                HttpMethod.POST,
                operatorRequest,
                ReviewResponse.class
        );

        // Klient tworzy recenzję dla operatora (klient jest author)
        ReviewRequest clientReview = ReviewRequest.builder()
                .stars(4)
                .body("Good operator!")
                .build();

        HttpEntity<ReviewRequest> clientRequest = new HttpEntity<>(clientReview, getHeaders(clientToken));
        testRestTemplate.exchange(
                "/api/reviews/createReview/" + order.getId() + "/" + operator.getId(),
                HttpMethod.POST,
                clientRequest,
                ReviewResponse.class
        );

        // When: Pobieramy recenzje klienta (gdzie klient jest target)
        HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<List<com.example.drones.reviews.dto.UserReviewResponse>> getResponse = testRestTemplate.exchange(
                "/api/reviews/getUserReviews/" + client.getId(),
                HttpMethod.GET,
                getEntity,
                new org.springframework.core.ParameterizedTypeReference<>() {}
        );

        // Then: Zwrócona jest tylko recenzja gdzie klient jest target
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody()).hasSize(1);

        com.example.drones.reviews.dto.UserReviewResponse review = getResponse.getBody().getFirst();
        assertThat(review.getStars()).isEqualTo(5);
        assertThat(review.getBody()).isEqualTo("Great client!");
    }

    @Test
    void givenUserWithMultipleReviewsWithDifferentRatings_whenGetUserReviews_thenReturnsAllWithCorrectData() {
        // Given: Operator z różnymi ocenami od klientów
        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        UserEntity operator = createTestOperator("operator_reviews5", service);

        String clientToken = registerAndLoginClient();
        UserEntity client = userRepository.findByEmail("client@example.com").orElseThrow();

        // Tworzenie 5 recenzji z różnymi ocenami
        for (int i = 1; i <= 5; i++) {
            OrdersEntity order = createCompletedOrder(client);

            ReviewRequest review = ReviewRequest.builder()
                    .stars(i)
                    .body("Review with " + i + " stars")
                    .build();

            HttpEntity<ReviewRequest> request = new HttpEntity<>(review, getHeaders(clientToken));
            testRestTemplate.exchange(
                    "/api/reviews/createReview/" + order.getId() + "/" + operator.getId(),
                    HttpMethod.POST,
                    request,
                    ReviewResponse.class
            );
        }

        // When: Pobieramy wszystkie recenzje operatora
        HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<List<com.example.drones.reviews.dto.UserReviewResponse>> getResponse = testRestTemplate.exchange(
                "/api/reviews/getUserReviews/" + operator.getId(),
                HttpMethod.GET,
                getEntity,
                new org.springframework.core.ParameterizedTypeReference<>() {}
        );

        // Then: Zwróconych jest 5 recenzji z prawidłowymi ocenami
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody()).hasSize(5);

        List<com.example.drones.reviews.dto.UserReviewResponse> reviews = getResponse.getBody();

        // Weryfikacja że są wszystkie oceny od 1 do 5
        for (int i = 1; i <= 5; i++) {
            final int stars = i;
            boolean hasReviewWithStars = reviews.stream()
                    .anyMatch(r -> r.getStars() == stars);
            assertThat(hasReviewWithStars).isTrue();
        }
    }

    @Test
    void givenUnauthenticatedUser_whenGetUserReviews_thenReturnsUnauthorized() {
        // Given: Nieuwierzytelniony użytkownik
        registerAndLoginClient();
        UserEntity client = userRepository.findByEmail("client@example.com").orElseThrow();

        // When: Próba pobrania recenzji bez tokenu
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> getEntity = new HttpEntity<>(headers);

        ResponseEntity<String> getResponse = testRestTemplate.exchange(
                "/api/reviews/getUserReviews/" + client.getId(),
                HttpMethod.GET,
                getEntity,
                String.class
        );

        // Then: Zwrócony błąd autoryzacji
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void givenUserWithReviewsWithoutBody_whenGetUserReviews_thenReturnsReviewsWithNullBody() {
        // Given: Klient z recenzją bez treści (tylko ocena)
        String clientToken = registerAndLoginClient();
        UserEntity client = userRepository.findByEmail("client@example.com").orElseThrow();

        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();
        createTestOperator("operator_reviews6", service);

        OrdersEntity order = createCompletedOrder(client);

        // Operator tworzy recenzję bez treści
        String operatorToken = loginAsOperator("operator_reviews6@op.pl");
        ReviewRequest review = ReviewRequest.builder()
                .stars(5)
                .build();

        HttpEntity<ReviewRequest> request = new HttpEntity<>(review, getHeaders(operatorToken));
        testRestTemplate.exchange(
                "/api/reviews/createReview/" + order.getId() + "/" + client.getId(),
                HttpMethod.POST,
                request,
                ReviewResponse.class
        );

        // When: Pobieramy recenzje klienta
        HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders(clientToken));
        ResponseEntity<List<com.example.drones.reviews.dto.UserReviewResponse>> getResponse = testRestTemplate.exchange(
                "/api/reviews/getUserReviews/" + client.getId(),
                HttpMethod.GET,
                getEntity,
                new org.springframework.core.ParameterizedTypeReference<>() {}
        );

        // Then: Recenzja została zwrócona z pustą treścią
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody()).hasSize(1);

        com.example.drones.reviews.dto.UserReviewResponse reviewResponse = getResponse.getBody().getFirst();
        assertThat(reviewResponse.getStars()).isEqualTo(5);
        assertThat(reviewResponse.getBody()).isNullOrEmpty();
    }
}
