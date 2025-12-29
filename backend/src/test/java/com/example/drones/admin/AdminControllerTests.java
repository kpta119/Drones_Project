package com.example.drones.admin;

import com.example.drones.admin.dto.UserDto;
import com.example.drones.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminControllerTests {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private UserDto userDto1;
    private UserDto userDto2;

    @BeforeEach
    public void setUp() {

        userDto1 = new UserDto(
                UUID.randomUUID().toString(),
                "testUser1",
                UserRole.CLIENT,
                "John",
                "Doe",
                "john.doe@example.com",
                "1234567890"
        );

        userDto2 = new UserDto(
                UUID.randomUUID().toString(),
                "testUser2",
                UserRole.OPERATOR,
                "Jane",
                "Smith",
                "jane.smith@example.com",
                "0987654321"
        );
    }

    @Test
    public void givenNoFilters_whenGetUsers_thenReturnsPageOfUsers() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<UserDto> expectedPage = new PageImpl<>(List.of(userDto1, userDto2), pageable, 2);
        when(adminService.getUsers(null, null, pageable)).thenReturn(expectedPage);

        ResponseEntity<Page<UserDto>> response = adminController.getUsers(null, null, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(2);
        assertThat(response.getBody().getContent()).containsExactly(userDto1, userDto2);
        verify(adminService).getUsers(null, null, pageable);
    }

    @Test
    public void givenQueryParameter_whenGetUsers_thenReturnsFilteredUsers() {
        String query = "john";
        Pageable pageable = PageRequest.of(0, 20);
        Page<UserDto> expectedPage = new PageImpl<>(List.of(userDto1), pageable, 1);
        when(adminService.getUsers(query, null, pageable)).thenReturn(expectedPage);

        ResponseEntity<Page<UserDto>> response = adminController.getUsers(query, null, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent()).containsExactly(userDto1);
        verify(adminService).getUsers(query, null, pageable);
    }

}
