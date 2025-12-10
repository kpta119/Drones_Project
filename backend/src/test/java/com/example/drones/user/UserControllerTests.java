package com.example.drones.user;

import com.example.drones.user.dto.UserResponse;
import com.example.drones.user.dto.UserUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTests {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserResponse mockUserResponse;
    private UserUpdateRequest mockUpdateRequest;
    private UUID userId;

    @BeforeEach
    public void setUp() {
        userId = UUID.randomUUID();
        mockUserResponse = UserResponse.builder()
                .username("testUser")
                .name("Jan")
                .surname("Kowalski")
                .email("jan@example.com")
                .role("CLIENT")
                .phoneNumber("123456789")
                .build();


        mockUpdateRequest = new UserUpdateRequest();
        mockUpdateRequest.setName("Janusz");
        mockUpdateRequest.setPhoneNumber("987654321");
    }


    @Test
    public void givenUserId_whenGetUserData_thenReturnsOkAndUser() {
        when(userService.getUserData(userId)).thenReturn(mockUserResponse);
        ResponseEntity<UserResponse> response = userController.getUserData(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockUserResponse.getUsername(), response.getBody().getUsername());
        verify(userService).getUserData(userId);
    }

    @Test
    public void givenNullUserId_whenGetUserData_thenReturnsOkAndCurrentUser() {
        when(userService.getUserData(null)).thenReturn(mockUserResponse);
        ResponseEntity<UserResponse> response = userController.getUserData(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockUserResponse, response.getBody());
        verify(userService).getUserData(null);
    }

    @Test
    public void givenNonExistentUser_whenGetUserData_thenThrowsException() {
        when(userService.getUserData(userId)).thenThrow(new RuntimeException("User not found"));
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userController.getUserData(userId);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userService).getUserData(userId);
    }


    @Test
    public void givenValidUpdateRequest_whenEditUserData_thenReturnsUpdatedUser() {
        when(userService.editUserData(mockUpdateRequest)).thenReturn(mockUserResponse);

        UserResponse response = userController.editUserData(mockUpdateRequest);

        assertNotNull(response);
        assertEquals(mockUserResponse.getName(), response.getName());
        verify(userService).editUserData(mockUpdateRequest);
    }

    @Test
    public void givenTakenEmail_whenEditUserData_thenThrowsException() {
        String errorMsg = "Email is already taken";
        doThrow(new RuntimeException(errorMsg)).when(userService).editUserData(mockUpdateRequest);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userController.editUserData(mockUpdateRequest);
        });

        assertEquals(errorMsg, exception.getMessage());
        verify(userService).editUserData(mockUpdateRequest);
    }
}