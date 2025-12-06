package com.example.drones.operators;

import com.example.drones.config.exceptions.UserNotFoundException;
import com.example.drones.operators.dto.OperatorAlreadyExistsException;
import com.example.drones.operators.dto.OperatorDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OperatorsControllerTests {

    @Mock
    private OperatorsService operatorsService;

    @InjectMocks
    private OperatorsController operatorsController;

    private OperatorDto validOperatorDto;

    @BeforeEach
    public void setUp() {
        validOperatorDto = OperatorDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(50)
                .certificates(List.of("UAV License"))
                .services(List.of("Aerial Photography"))
                .build();
    }

    @Test
    public void givenValidOperatorDto_whenCreateOperatorProfile_thenReturnsCreated() {
        OperatorDto request = validOperatorDto;
        when(operatorsService.createProfile(request)).thenReturn(request);

        ResponseEntity<OperatorDto> response = operatorsController.createOperatorProfile(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(request);
        verify(operatorsService).createProfile(request);
    }

    @Test
    public void givenUserNotFound_whenCreateOperatorProfile_thenThrowsUserNotFoundException() {
        OperatorDto request = validOperatorDto;
        when(operatorsService.createProfile(request)).thenThrow(new UserNotFoundException());

        assertThatThrownBy(() -> operatorsController.createOperatorProfile(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
        verify(operatorsService).createProfile(request);
    }

    @Test
    public void givenUserAlreadyOperator_whenCreateOperatorProfile_thenThrowsOperatorAlreadyExistsException() {
        OperatorDto request = validOperatorDto;
        when(operatorsService.createProfile(request)).thenThrow(new OperatorAlreadyExistsException());

        assertThatThrownBy(() -> operatorsController.createOperatorProfile(request))
                .isInstanceOf(OperatorAlreadyExistsException.class)
                .hasMessage("Operator profile already exists for this user.");
        verify(operatorsService).createProfile(request);
    }
}
