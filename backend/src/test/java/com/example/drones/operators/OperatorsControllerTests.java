package com.example.drones.operators;

import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.operators.dto.CreateOperatorProfileDto;
import com.example.drones.operators.dto.OperatorProfileDto;
import com.example.drones.operators.exceptions.NoSuchOperatorException;
import com.example.drones.operators.exceptions.OperatorAlreadyExistsException;
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

    private CreateOperatorProfileDto validCreateOperatorDto;
    private OperatorProfileDto response;

    @BeforeEach
    public void setUp() {
        validCreateOperatorDto = CreateOperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(50)
                .certificates(List.of("UAV License"))
                .services(List.of("Aerial Photography"))
                .build();

        response = OperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(50)
                .certificates(List.of("UAV License"))
                .services(List.of("Aerial Photography"))
                .build();
    }

    @Test
    public void givenValidOperatorDto_whenCreateOperatorProfile_thenReturnsCreated() {
        CreateOperatorProfileDto request = validCreateOperatorDto;
        when(operatorsService.createProfile(request)).thenReturn(response);

        ResponseEntity<OperatorProfileDto> response = operatorsController.createOperatorProfile(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(this.response);
        verify(operatorsService).createProfile(request);
    }

    @Test
    public void givenUserNotFound_whenCreateOperatorProfile_thenThrowsUserNotFoundException() {
        CreateOperatorProfileDto request = validCreateOperatorDto;
        when(operatorsService.createProfile(request)).thenThrow(new UserNotFoundException());

        assertThatThrownBy(() -> operatorsController.createOperatorProfile(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
        verify(operatorsService).createProfile(request);
    }

    @Test
    public void givenUserAlreadyOperator_whenCreateOperatorProfile_thenThrowsOperatorAlreadyExistsException() {
        CreateOperatorProfileDto request = validCreateOperatorDto;
        when(operatorsService.createProfile(request)).thenThrow(new OperatorAlreadyExistsException());

        assertThatThrownBy(() -> operatorsController.createOperatorProfile(request))
                .isInstanceOf(OperatorAlreadyExistsException.class)
                .hasMessage("Operator profile already exists for this user.");
        verify(operatorsService).createProfile(request);
    }

    @Test
    public void givenValidOperatorDto_whenEditOperatorProfile_thenReturnsAccepted() {
        OperatorProfileDto request = response;
        OperatorProfileDto updatedResponse = OperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(100)
                .certificates(List.of("UAV License", "Advanced License"))
                .services(List.of("Aerial Photography", "Surveillance"))
                .build();
        when(operatorsService.editProfile(request)).thenReturn(updatedResponse);

        ResponseEntity<OperatorProfileDto> response = operatorsController.editOperatorProfile(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isEqualTo(updatedResponse);
        verify(operatorsService).editProfile(request);
    }

    @Test
    public void givenUserNotFound_whenEditOperatorProfile_thenThrowsUserNotFoundException() {
        OperatorProfileDto request = response;
        when(operatorsService.editProfile(request)).thenThrow(new UserNotFoundException());

        assertThatThrownBy(() -> operatorsController.editOperatorProfile(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
        verify(operatorsService).editProfile(request);
    }

    @Test
    public void givenUserNotOperator_whenEditOperatorProfile_thenThrowsNoSuchOperatorException() {
        OperatorProfileDto request = response;
        when(operatorsService.editProfile(request)).thenThrow(new NoSuchOperatorException());

        assertThatThrownBy(() -> operatorsController.editOperatorProfile(request))
                .isInstanceOf(NoSuchOperatorException.class)
                .hasMessage("No operator profile found for this user.");
        verify(operatorsService).editProfile(request);
    }
}
