package com.example.drones.operators;

import com.example.drones.common.config.JwtService;
import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.operators.dto.CreateOperatorProfileDto;
import com.example.drones.operators.dto.OperatorProfileDto;
import com.example.drones.operators.exceptions.OperatorAlreadyExistsException;
import com.example.drones.services.OperatorServicesService;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserMapper;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OperatorsServiceTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private OperatorServicesService operatorServicesService;
    @Mock
    private UserMapper operatorMapper;

    @InjectMocks
    private OperatorsService service;

    @Test
    public void givenValidOperatorDto_whenCreateProfile_thenProfileCreatedAndReturnsDto() {
        UUID userId = UUID.randomUUID();
        CreateOperatorProfileDto operatorDto = CreateOperatorProfileDto.builder()
                .coordinates("45.0,90.0")
                .radius(10)
                .certificates(List.of("Cert1", "Cert2"))
                .services(List.of("Delivery", "Surveillance"))
                .build();
        OperatorProfileDto expectedDto = OperatorProfileDto.builder()
                .coordinates("45.0,90.0")
                .radius(10)
                .certificates(List.of("Cert1", "Cert2"))
                .services(List.of("Delivery", "Surveillance"))
                .build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .role(UserRole.CLIENT)
                .build();


        when(jwtService.extractUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);
        when(operatorServicesService.addOperatorServices(any(UserEntity.class), anyList()))
                .thenReturn(operatorDto.services());
        when(operatorMapper.toOperatorProfileDto(any(UserEntity.class), anyList()))
                .thenReturn(expectedDto);

        OperatorProfileDto result = service.createProfile(operatorDto);

        verify(jwtService).extractUserId();
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(UserEntity.class));
        verify(operatorServicesService).addOperatorServices(any(UserEntity.class), eq(operatorDto.services()));
        verify(operatorMapper).toOperatorProfileDto(any(UserEntity.class), eq(operatorDto.services()));

        assertThat(user.getRole()).isEqualTo(UserRole.OPERATOR);
        assertThat(user.getCoordinates()).isEqualTo(operatorDto.coordinates());
        assertThat(user.getRadius()).isEqualTo(operatorDto.radius());
        assertThat(user.getCertificates()).isEqualTo(operatorDto.certificates());
        assertThat(result).isEqualTo(expectedDto);
    }

    @Test
    public void givenOperatorDtoWithNoCertificates_whenCreateProfile_thenProfileCreatedWithNullCertificates() {
        UUID userId = UUID.randomUUID();
        CreateOperatorProfileDto operatorDto = CreateOperatorProfileDto.builder()
                .coordinates("45.0,90.0")
                .radius(10)
                .certificates(List.of())
                .services(List.of("Delivery"))
                .build();
        OperatorProfileDto expectedDto = OperatorProfileDto.builder()
                .coordinates("45.0,90.0")
                .radius(10)
                .certificates(List.of())
                .services(List.of("Delivery"))
                .build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .role(UserRole.CLIENT)
                .build();

        when(jwtService.extractUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);
        when(operatorServicesService.addOperatorServices(any(UserEntity.class), anyList()))
                .thenReturn(operatorDto.services());
        when(operatorMapper.toOperatorProfileDto(any(UserEntity.class), anyList()))
                .thenReturn(expectedDto);

        OperatorProfileDto result = service.createProfile(operatorDto);

        assertThat(user.getCertificates()).isEmpty();
        assertThat(result).isEqualTo(expectedDto);
        verify(userRepository).save(any(UserEntity.class));
        verify(operatorServicesService).addOperatorServices(any(UserEntity.class), eq(operatorDto.services()));
        verify(operatorMapper).toOperatorProfileDto(any(UserEntity.class), eq(operatorDto.services()));
    }

    @Test
    public void givenUserNotFound_whenCreateProfile_thenThrowsUserNotFoundException() {
        UUID userId = UUID.randomUUID();
        CreateOperatorProfileDto operatorDto = CreateOperatorProfileDto.builder()
                .coordinates("45.0,90.0")
                .radius(10)
                .certificates(List.of("Cert1"))
                .services(List.of("Delivery"))
                .build();

        when(jwtService.extractUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createProfile(operatorDto))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(jwtService).extractUserId();
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
        verify(operatorServicesService, never()).addOperatorServices(any(), any());
    }

    @Test
    public void givenUserAlreadyOperator_whenCreateProfile_thenThrowsOperatorAlreadyExistsException() {
        UUID userId = UUID.randomUUID();
        CreateOperatorProfileDto operatorDto = CreateOperatorProfileDto.builder()
                .coordinates("45.0,90.0")
                .radius(10)
                .certificates(List.of("Cert1"))
                .services(List.of("Delivery"))
                .build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .role(UserRole.OPERATOR)
                .coordinates("40.0,80.0")
                .radius(5)
                .build();

        when(jwtService.extractUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.createProfile(operatorDto))
                .isInstanceOf(OperatorAlreadyExistsException.class)
                .hasMessage("Operator profile already exists for this user.");

        verify(jwtService).extractUserId();
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
        verify(operatorServicesService, never()).addOperatorServices(any(), any());
    }
}

