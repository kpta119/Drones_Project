package com.example.drones.admin;

import com.example.drones.admin.dto.SystemStatsDto;
import com.example.drones.admin.dto.UserDto;
import com.example.drones.admin.exceptions.NoSuchUserException;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTests {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private AdminMapper adminMapper;

    @InjectMocks
    private AdminService adminService;

    private UUID testUserId;
    private UserEntity testUserEntity;
    private UserDto testUserDto;

    @BeforeEach
    public void setUp() {
        testUserId = UUID.randomUUID();

        testUserEntity = UserEntity.builder()
                .id(testUserId)
                .displayName("testUser")
                .name("John")
                .surname("Doe")
                .email("john.doe@example.com")
                .phoneNumber("1234567890")
                .password("hashedPassword")
                .role(UserRole.CLIENT)
                .build();

        testUserDto = new UserDto(
                testUserId.toString(),
                "testUser",
                UserRole.BLOCKED,
                "John",
                "Doe",
                "john.doe@example.com",
                "1234567890"
        );
    }

    @Test
    public void givenValidUserId_whenBanUser_thenReturnsBannedUserDto() {
        when(adminRepository.findById(testUserId)).thenReturn(Optional.of(testUserEntity));
        when(adminMapper.toUserDto(any(UserEntity.class))).thenReturn(testUserDto);

        UserDto result = adminService.banUser(testUserId);

        assertThat(result).isNotNull();
        assertThat(result.role()).isEqualTo(UserRole.BLOCKED);
        assertThat(result.id()).isEqualTo(testUserId.toString());
        assertThat(result.displayName()).isEqualTo("testUser");
        assertThat(result.email()).isEqualTo("john.doe@example.com");
    }

    @Test
    public void givenNonExistentUserId_whenBanUser_thenThrowsNoSuchUserException() {
        UUID nonExistentUserId = UUID.randomUUID();
        when(adminRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        NoSuchUserException exception = assertThrows(
                NoSuchUserException.class,
                () -> adminService.banUser(nonExistentUserId)
        );

        assertThat(exception.getMessage()).isEqualTo("No such user exists");
        verify(adminRepository).findById(nonExistentUserId);
        verify(adminRepository, never()).save(any(UserEntity.class));
        verify(adminMapper, never()).toUserDto(any(UserEntity.class));
    }

    @Test
    public void givenOperatorUser_whenBanUser_thenOperatorIsBanned() {
        UserEntity operatorEntity = UserEntity.builder()
                .id(testUserId)
                .displayName("operatorUser")
                .name("Jane")
                .surname("Smith")
                .email("jane.smith@example.com")
                .phoneNumber("0987654321")
                .password("hashedPassword")
                .role(UserRole.OPERATOR)
                .build();

        UserDto bannedOperatorDto = new UserDto(
                testUserId.toString(),
                "operatorUser",
                UserRole.BLOCKED,
                "Jane",
                "Smith",
                "jane.smith@example.com",
                "0987654321"
        );

        when(adminRepository.findById(testUserId)).thenReturn(Optional.of(operatorEntity));
        when(adminMapper.toUserDto(any(UserEntity.class))).thenReturn(bannedOperatorDto);

        UserDto result = adminService.banUser(testUserId);

        assertThat(operatorEntity.getRole()).isEqualTo(UserRole.BLOCKED);
        assertThat(result.role()).isEqualTo(UserRole.BLOCKED);
        verify(adminRepository).save(operatorEntity);
    }

    @Test
    public void givenAlreadyBannedUser_whenBanUser_thenStillSetsRoleToBlocked() {
        UserEntity bannedEntity = UserEntity.builder()
                .id(testUserId)
                .displayName("bannedUser")
                .name("John")
                .surname("Doe")
                .email("john.doe@example.com")
                .phoneNumber("1234567890")
                .password("hashedPassword")
                .role(UserRole.BLOCKED)
                .build();

        when(adminRepository.findById(testUserId)).thenReturn(Optional.of(bannedEntity));
        when(adminMapper.toUserDto(any(UserEntity.class))).thenReturn(testUserDto);

        UserDto result = adminService.banUser(testUserId);

        assertThat(bannedEntity.getRole()).isEqualTo(UserRole.BLOCKED);
        assertThat(result.role()).isEqualTo(UserRole.BLOCKED);
        verify(adminRepository).save(bannedEntity);
    }

    @Test
    public void givenZeroOperators_whenGetSystemStats_thenAvgPerOperatorIsZero() {
        SystemStatsProjection mockProjection = mock(SystemStatsProjection.class);
        when(mockProjection.getClientsCount()).thenReturn(100L);
        when(mockProjection.getOperatorsCount()).thenReturn(0L);
        when(mockProjection.getActiveOrders()).thenReturn(50L);
        when(mockProjection.getCompletedOrders()).thenReturn(200L);
        when(mockProjection.getBusyOperators()).thenReturn(0L);
        when(mockProjection.getTopOperatorId()).thenReturn(null);
        when(mockProjection.getTotalReviews()).thenReturn(100L);

        when(adminRepository.getSystemStatistics()).thenReturn(mockProjection);

        SystemStatsDto stats = adminService.getSystemStats();

        assertThat(stats.getOrders().getAvgPerOperator()).isEqualTo(0.0);
        assertThat(stats.getUsers().getOperators()).isEqualTo(0L);
        assertThat(stats.getUsers().getClients()).isEqualTo(100L);
        verify(adminRepository).getSystemStatistics();
    }

    @Test
    public void givenActiveOrdersAndOperators_whenGetSystemStats_thenAvgPerOperatorIsRoundedToOneDecimal() {
        Long operatorsCount = 3L;
        Long activeOrders = 10L;
        Double expectedAvg = 3.3;
        UUID topOperatorId = UUID.randomUUID();

        SystemStatsProjection mockProjection = mock(SystemStatsProjection.class);
        when(mockProjection.getClientsCount()).thenReturn(100L);
        when(mockProjection.getOperatorsCount()).thenReturn(operatorsCount);
        when(mockProjection.getActiveOrders()).thenReturn(activeOrders);
        when(mockProjection.getCompletedOrders()).thenReturn(200L);
        when(mockProjection.getBusyOperators()).thenReturn(2L);
        when(mockProjection.getTopOperatorId()).thenReturn(topOperatorId);
        when(mockProjection.getTopOperatorCompletedOrders()).thenReturn(50L);
        when(mockProjection.getTotalReviews()).thenReturn(100L);

        when(adminRepository.getSystemStatistics()).thenReturn(mockProjection);

        SystemStatsDto stats = adminService.getSystemStats();

        assertThat(stats.getOrders().getAvgPerOperator()).isEqualTo(expectedAvg);
        assertThat(stats.getOrders().getActive()).isEqualTo(activeOrders);
        assertThat(stats.getUsers().getOperators()).isEqualTo(operatorsCount);
        verify(adminRepository).getSystemStatistics();
    }
}
