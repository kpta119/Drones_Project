package com.example.drones.admin;

import com.example.drones.common.config.auth.JwtService;
import com.example.drones.orders.OrderStatus;
import com.example.drones.orders.OrdersEntity;
import com.example.drones.orders.OrdersRepository;
import com.example.drones.services.ServicesEntity;
import com.example.drones.services.ServicesRepository;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class AdminIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private ServicesRepository servicesRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserEntity clientUser;
    private UserEntity operatorUser;
    private String adminToken;
    private String clientToken;

    @BeforeEach
    void setUp() {
        UserEntity adminUser = UserEntity.builder()
                .displayName("adminUser")
                .name("Admin")
                .surname("User")
                .email("admin@example.com")
                .password(passwordEncoder.encode("adminPass123"))
                .phoneNumber("111111111")
                .role(UserRole.ADMIN)
                .build();
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateToken(adminUser.getId());

        clientUser = UserEntity.builder()
                .displayName("clientUser")
                .name("John")
                .surname("Doe")
                .email("client@example.com")
                .password(passwordEncoder.encode("clientPass123"))
                .phoneNumber("222222222")
                .role(UserRole.CLIENT)
                .build();
        clientUser = userRepository.save(clientUser);
        clientToken = jwtService.generateToken(clientUser.getId());

        operatorUser = UserEntity.builder()
                .displayName("operatorUser")
                .name("Jane")
                .surname("Smith")
                .email("operator@example.com")
                .password(passwordEncoder.encode("operatorPass123"))
                .phoneNumber("333333333")
                .role(UserRole.OPERATOR)
                .build();
        operatorUser = userRepository.save(operatorUser);
    }

    @Test
    void givenAdminUser_whenGetUsers_thenReturnsPageOfUsers() throws Exception {
        mockMvc.perform(get("/api/admin/getUsers")
                        .header("X-USER-TOKEN", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void givenAdminUser_whenGetUsersWithQueryParameter_thenReturnsFilteredUsers() throws Exception {
        mockMvc.perform(get("/api/admin/getUsers")
                        .param("query", "client")
                        .header("X-USER-TOKEN", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void givenAdminUser_whenGetUsersWithRoleFilter_thenReturnsUsersWithSpecificRole() throws Exception {
        mockMvc.perform(get("/api/admin/getUsers")
                        .param("role", "OPERATOR")
                        .header("X-USER-TOKEN", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void givenAdminUser_whenGetUsersWithPagination_thenReturnsPagedResults() throws Exception {
        mockMvc.perform(get("/api/admin/getUsers")
                        .param("page", "0")
                        .param("size", "2")
                        .header("X-USER-TOKEN", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void givenNonAdminUser_whenGetUsers_thenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/getUsers")
                        .header("X-USER-TOKEN", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenUnauthenticatedUser_whenGetUsers_thenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/getUsers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAdminUser_whenBanUser_thenUserRoleIsSetToBlocked() throws Exception {
        UUID clientId = clientUser.getId();

        mockMvc.perform(patch("/api/admin/banUser/{userId}", clientId)
                        .header("X-USER-TOKEN", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("BLOCKED"))
                .andExpect(jsonPath("$.id").value(clientId.toString()));

        UserEntity updatedClient = userRepository.findById(clientId).orElseThrow();
        assertThat(updatedClient.getRole()).isEqualTo(UserRole.BLOCKED);
    }

    @Test
    void givenAdminUser_whenBanNonExistentUser_thenReturnsNotFound() throws Exception {
        UUID nonExistentUserId = UUID.randomUUID();

        mockMvc.perform(patch("/api/admin/banUser/{userId}", nonExistentUserId)
                        .header("X-USER-TOKEN", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No such user exists"));
    }

    @Test
    void givenNonAdminUser_whenBanUser_thenReturnsForbidden() throws Exception {
        UUID operatorId = operatorUser.getId();

        mockMvc.perform(patch("/api/admin/banUser/{userId}", operatorId)
                        .header("X-USER-TOKEN", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenUnauthenticatedUser_whenBanUser_thenReturnsForbidden() throws Exception {
        UUID clientId = clientUser.getId();

        mockMvc.perform(patch("/api/admin/banUser/{userId}", clientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }


    @Test
    void givenAdminUser_whenBanOperator_thenOperatorIsBanned() throws Exception {
        UUID operatorId = operatorUser.getId();

        mockMvc.perform(patch("/api/admin/banUser/{userId}", operatorId)
                        .header("X-USER-TOKEN", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("BLOCKED"));

        UserEntity updatedOperator = userRepository.findById(operatorId).orElseThrow();
        assertThat(updatedOperator.getRole()).isEqualTo(UserRole.BLOCKED);
    }

    @Test
    void givenAdminUser_whenGetOrders_thenReturnsPageOfOrders() throws Exception {
        ServicesEntity service = new ServicesEntity("Photography");
        servicesRepository.saveAndFlush(service);

        OrdersEntity order1 = OrdersEntity.builder()
                .title("Aerial Photography Order")
                .description("Need professional aerial photos")
                .service(service)
                .coordinates("52.2297,21.0122")
                .fromDate(java.time.LocalDateTime.now().plusDays(1))
                .toDate(java.time.LocalDateTime.now().plusDays(2))
                .status(OrderStatus.OPEN)
                .user(clientUser)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        ordersRepository.saveAndFlush(order1);

        OrdersEntity order2 = OrdersEntity.builder()
                .title("Land Survey Order")
                .description("Topographic survey needed")
                .service(service)
                .coordinates("52.2400,21.0300")
                .fromDate(java.time.LocalDateTime.now().plusDays(3))
                .toDate(java.time.LocalDateTime.now().plusDays(4))
                .status(OrderStatus.IN_PROGRESS)
                .user(clientUser)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        ordersRepository.saveAndFlush(order2);

        mockMvc.perform(get("/api/admin/getOrders")
                        .header("X-USER-TOKEN", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void givenAdminUser_whenGetOrdersWithPagination_thenReturnsPagedResults() throws Exception {
        ServicesEntity service = new ServicesEntity("Surveying");
        servicesRepository.saveAndFlush(service);

        for (int i = 0; i < 5; i++) {
            OrdersEntity order = OrdersEntity.builder()
                    .title("Order " + i)
                    .description("Description " + i)
                    .service(service)
                    .coordinates("52.23,21.01")
                    .fromDate(java.time.LocalDateTime.now().plusDays(i + 1))
                    .toDate(java.time.LocalDateTime.now().plusDays(i + 2))
                    .status(OrderStatus.OPEN)
                    .user(clientUser)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            ordersRepository.saveAndFlush(order);
        }

        mockMvc.perform(get("/api/admin/getOrders")
                        .param("page", "0")
                        .param("size", "2")
                        .header("X-USER-TOKEN", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(5))
                .andExpect(jsonPath("$.page.size").value(2));
    }

    @Test
    void givenNonAdminUser_whenGetOrders_thenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/getOrders")
                        .header("X-USER-TOKEN", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenUnauthenticatedUser_whenGetOrders_thenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/getOrders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAdminUser_whenGetOrdersWithNoOrders_thenReturnsEmptyPage() throws Exception {
        ordersRepository.deleteAll();
        ordersRepository.flush();

        mockMvc.perform(get("/api/admin/getOrders")
                        .header("X-USER-TOKEN", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

}
