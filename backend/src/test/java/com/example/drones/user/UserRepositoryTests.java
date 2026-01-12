package com.example.drones.user;

import com.example.drones.services.OperatorServicesEntity;
import com.example.drones.services.OperatorServicesRepository;
import com.example.drones.services.ServicesEntity;
import com.example.drones.services.ServicesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
class UserRepositoryTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServicesRepository servicesRepository;

    @Autowired
    private OperatorServicesRepository operatorServicesRepository;

    private final String SERVICE_NAME = "Filmowanie";
    private UserEntity clientUser;

    @BeforeEach
    void setUp() {
        ServicesEntity s1 = new ServicesEntity();
        s1.setName(SERVICE_NAME);
        ServicesEntity s2 = new ServicesEntity();
        String OTHER_SERVICE = "Kopanie";
        s2.setName(OTHER_SERVICE);
        servicesRepository.saveAll(List.of(s1, s2));

        // Create a client user who will create the order
        clientUser = UserEntity.builder()
                .displayName("client")
                .email("client@test.com")
                .role(UserRole.CLIENT)
                .name("Client").surname("User").password("pass")
                .build();
        userRepository.save(clientUser);

        createOperator("operator_ok", "52.2300, 21.0100", 10, SERVICE_NAME);

        createOperator("operator_far", "54.3520, 18.6466", 10, SERVICE_NAME);

        createOperator("operator_wrong_service", "52.2300, 21.0100", 10, OTHER_SERVICE);

        createOperator("operator_small_radius", "52.2700, 21.0500", 1, SERVICE_NAME);
    }

    private void createOperator(String username, String coords, int radius, String serviceName) {
        UserEntity user = UserEntity.builder()
                .displayName(username)
                .email(username + "@test.com")
                .role(UserRole.OPERATOR)
                .name("Test").surname("Op").password("pass")
                .coordinates(coords)
                .radius(radius)
                .build();

        userRepository.save(user);

        ServicesEntity service = servicesRepository.findById(serviceName).orElseThrow();
        OperatorServicesEntity link = new OperatorServicesEntity();
        link.setOperator(user);
        link.setService(service);
        link.setServiceName(serviceName);
        operatorServicesRepository.save(link);
    }

    @Test
    void shouldFindOnlyMatchingOperator() {
        double orderLat = 52.2297;
        double orderLon = 21.0122;

        List<UserEntity> result = userRepository.findMatchingOperators(SERVICE_NAME, orderLat, orderLon, clientUser.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDisplayName()).isEqualTo("operator_ok");
    }
}