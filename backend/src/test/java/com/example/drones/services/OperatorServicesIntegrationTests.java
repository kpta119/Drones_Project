package com.example.drones.services;

import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OperatorServicesIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private OperatorServicesService operatorServicesService;

    @Autowired
    private OperatorServicesRepository operatorServicesRepository;

    @Autowired
    private ServicesRepository servicesRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity testOperator;
    private static final String SERVICE_1 = "Aerial Photography";
    private static final String SERVICE_2 = "Laser Scanning";
    private static final String SERVICE_3 = "Thermal Imaging";

    @BeforeEach
    void setUp() {
        // Create test services if they don't exist
        if (!servicesRepository.existsById(SERVICE_1)) {
            servicesRepository.save(new ServicesEntity(SERVICE_1));
        }
        if (!servicesRepository.existsById(SERVICE_2)) {
            servicesRepository.save(new ServicesEntity(SERVICE_2));
        }
        if (!servicesRepository.existsById(SERVICE_3)) {
            servicesRepository.save(new ServicesEntity(SERVICE_3));
        }

        // Create test operator
        testOperator = UserEntity.builder()
                .displayName("TestOperator")
                .name("John")
                .surname("Doe")
                .email("operator@test.com")
                .password("password123")
                .phoneNumber("123456789")
                .role(UserRole.OPERATOR)
                .build();
        testOperator = userRepository.save(testOperator);
    }

    @AfterEach
    void tearDown() {
        operatorServicesRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void givenOperatorAndServices_whenAddOperatorServices_thenServicesAreAddedSuccessfully() {
        List<String> servicesToAdd = Arrays.asList(SERVICE_1, SERVICE_2);

        List<String> addedServices = operatorServicesService.addOperatorServices(testOperator, servicesToAdd);

        assertThat(addedServices).hasSize(2);
        assertThat(addedServices).containsExactlyInAnyOrder(SERVICE_1, SERVICE_2);

        List<OperatorServicesEntity> savedEntities = operatorServicesRepository.findAllByOperatorId(testOperator.getId());
        assertThat(savedEntities).hasSize(2);
        assertThat(savedEntities)
                .extracting(OperatorServicesEntity::getServiceName)
                .containsExactlyInAnyOrder(SERVICE_1, SERVICE_2);
    }

    @Test
    void givenOperatorWithNoServices_whenGetOperatorServices_thenReturnsEmptyList() {
        List<String> services = operatorServicesService.getOperatorServices(testOperator);

        assertThat(services).isEmpty();
    }

    @Test
    void givenOperatorWithServices_whenGetOperatorServices_thenReturnsAllServices() {
        operatorServicesService.addOperatorServices(testOperator, Arrays.asList(SERVICE_1, SERVICE_2, SERVICE_3));

        List<String> retrievedServices = operatorServicesService.getOperatorServices(testOperator);

        assertThat(retrievedServices).hasSize(3);
        assertThat(retrievedServices).containsExactlyInAnyOrder(SERVICE_1, SERVICE_2, SERVICE_3);
    }

    @Test
    void givenOperatorWithExistingServices_whenEditOperatorServices_thenOldServicesAreReplacedWithNew() {
        operatorServicesService.addOperatorServices(testOperator, Arrays.asList(SERVICE_1, SERVICE_3));
        assertThat(operatorServicesRepository.findAllByOperatorId(testOperator.getId())).hasSize(2);

        List<String> newServices = List.of(SERVICE_2);
        List<String> editedServices = operatorServicesService.editOperatorServices(testOperator, newServices);

        assertThat(editedServices).hasSize(1);
        assertThat(editedServices).containsExactly(SERVICE_2);

        List<OperatorServicesEntity> savedEntities = operatorServicesRepository.findAllByOperatorId(testOperator.getId());
        assertThat(savedEntities).hasSize(1);
        assertThat(savedEntities)
                .extracting(OperatorServicesEntity::getServiceName)
                .containsExactly(SERVICE_2)
                .doesNotContain(SERVICE_1, SERVICE_3);
    }

    @Test
    void givenOperatorWithServices_whenEditToEmptyList_thenAllServicesAreRemoved() {
        operatorServicesService.addOperatorServices(testOperator, Arrays.asList(SERVICE_1, SERVICE_2));
        assertThat(operatorServicesRepository.findAllByOperatorId(testOperator.getId())).hasSize(2);

        List<String> editedServices = operatorServicesService.editOperatorServices(testOperator, List.of());

        assertThat(editedServices).isEmpty();
        assertThat(operatorServicesRepository.findAllByOperatorId(testOperator.getId())).isEmpty();
    }

    @Test
    void givenOperator_whenEditToOneMoreService_thenNewServiceIsAdded() {
        operatorServicesService.addOperatorServices(testOperator, List.of(SERVICE_1));
        assertThat(operatorServicesRepository.findAllByOperatorId(testOperator.getId())).hasSize(1);

        List<String> editedServices = operatorServicesService.editOperatorServices(testOperator, List.of(SERVICE_1, SERVICE_2));

        assertThat(editedServices).hasSize(2);
        assertThat(editedServices).containsExactlyInAnyOrder(SERVICE_1, SERVICE_2);

        List<OperatorServicesEntity> savedEntities = operatorServicesRepository.findAllByOperatorId(testOperator.getId());
        assertThat(savedEntities).hasSize(2);
        assertThat(savedEntities)
                .extracting(OperatorServicesEntity::getServiceName)
                .containsExactlyInAnyOrder(SERVICE_1, SERVICE_2);
    }
}
