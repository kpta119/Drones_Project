package com.example.drones.services;

import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OperatorServicesServiceUnitTests {

    @Mock
    private OperatorServicesRepository operatorServicesRepository;

    @InjectMocks
    private OperatorServicesService operatorServicesService;

    private UserEntity operator;

    @BeforeEach
    public void setUp() {
        operator = UserEntity.builder()
                .id(UUID.randomUUID())
                .role(UserRole.OPERATOR)
                .email("operator@example.com")
                .build();
    }

    @Test
    public void givenOperatorAndServices_whenAddOperatorServices_thenServicesAreSaved() {
        List<String> services = List.of("Delivery", "Surveillance");
        OperatorServicesEntity entity1 = createOperatorServiceEntity(1, "Delivery", operator);
        OperatorServicesEntity entity2 = createOperatorServiceEntity(2, "Surveillance", operator);

        when(operatorServicesRepository.save(any(OperatorServicesEntity.class)))
                .thenReturn(entity1, entity2);

        List<String> result = operatorServicesService.addOperatorServices(operator, services);
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly("Delivery", "Surveillance");
        ArgumentCaptor<OperatorServicesEntity> captor = ArgumentCaptor.forClass(OperatorServicesEntity.class);
        verify(operatorServicesRepository, times(2)).save(captor.capture());

        List<OperatorServicesEntity> capturedEntities = captor.getAllValues();
        assertThat(capturedEntities.get(0).getServiceName()).isEqualTo("Delivery");
        assertThat(capturedEntities.get(0).getOperator()).isEqualTo(operator);
        assertThat(capturedEntities.get(1).getServiceName()).isEqualTo("Surveillance");
        assertThat(capturedEntities.get(1).getOperator()).isEqualTo(operator);
    }

    @Test
    public void givenOperatorAndEmptyServicesList_whenAddOperatorServices_thenReturnsEmptyList() {
        List<String> services = List.of();

        List<String> result = operatorServicesService.addOperatorServices(operator, services);

        assertThat(result).isEmpty();
        verify(operatorServicesRepository, never()).save(any(OperatorServicesEntity.class));
    }

    @Test
    public void givenOperatorAndServices_whenEditOperatorServices_thenOldServicesDeletedAndNewServicesAdded() {
        List<String> newServices = List.of("Photography", "Mapping");
        OperatorServicesEntity entity1 = createOperatorServiceEntity(1, "Photography", operator);
        OperatorServicesEntity entity2 = createOperatorServiceEntity(2, "Mapping", operator);

        when(operatorServicesRepository.save(any(OperatorServicesEntity.class)))
                .thenReturn(entity1, entity2);

        List<String> result = operatorServicesService.editOperatorServices(operator, newServices);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly("Photography", "Mapping");
        verify(operatorServicesRepository).deleteAllByOperator(operator);
        verify(operatorServicesRepository, times(2)).save(any(OperatorServicesEntity.class));
    }

    @Test
    public void givenOperatorAndEmptyServicesList_whenEditOperatorServices_thenOldServicesDeletedAndNoNewServicesAdded() {
        List<String> newServices = List.of();

        List<String> result = operatorServicesService.editOperatorServices(operator, newServices);

        assertThat(result).isEmpty();
        verify(operatorServicesRepository).deleteAllByOperator(operator);
        verify(operatorServicesRepository, never()).save(any(OperatorServicesEntity.class));
    }

    @Test
    public void givenOperatorWithServices_whenGetOperatorServices_thenReturnsServiceNames() {
        OperatorServicesEntity entity1 = createOperatorServiceEntity(1, "Delivery", operator);
        OperatorServicesEntity entity2 = createOperatorServiceEntity(2, "Surveillance", operator);
        OperatorServicesEntity entity3 = createOperatorServiceEntity(3, "Inspection", operator);
        List<OperatorServicesEntity> entities = List.of(entity1, entity2, entity3);

        when(operatorServicesRepository.findAllByOperator(operator)).thenReturn(entities);

        List<String> result = operatorServicesService.getOperatorServices(operator);

        assertThat(result).hasSize(3);
        assertThat(result).containsExactly("Delivery", "Surveillance", "Inspection");
        verify(operatorServicesRepository).findAllByOperator(operator);
    }

    @Test
    public void givenOperatorWithNoServices_whenGetOperatorServices_thenReturnsEmptyList() {
        when(operatorServicesRepository.findAllByOperator(operator)).thenReturn(List.of());

        List<String> result = operatorServicesService.getOperatorServices(operator);

        assertThat(result).isEmpty();
        verify(operatorServicesRepository).findAllByOperator(operator);
    }

    private OperatorServicesEntity createOperatorServiceEntity(Integer id, String serviceName, UserEntity operator) {
        OperatorServicesEntity entity = new OperatorServicesEntity();
        entity.setId(id);
        entity.setServiceName(serviceName);
        entity.setOperator(operator);
        entity.setOperatorId(operator.getId());
        return entity;
    }
}

