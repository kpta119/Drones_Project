package com.example.drones.admin;

import com.example.drones.admin.dto.OrderDto;
import com.example.drones.admin.dto.SystemStatsDto;
import com.example.drones.admin.dto.UserDto;
import com.example.drones.admin.exceptions.NoSuchUserException;
import com.example.drones.orders.MatchedOrderStatus;
import com.example.drones.orders.OrderStatus;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class AdminService {
    private final AdminRepository adminRepository;
    private final AdminMapper adminMapper;

    public Page<UserDto> getUsers(String query, UserRole role, Pageable pageable) {
        return adminRepository.findAllByQueryAndRole(query, role, pageable);
    }

    public UserDto banUser(UUID userId) {
        UserEntity user = adminRepository.findById(userId)
                .orElseThrow(NoSuchUserException::new);

        user.setRole(UserRole.BLOCKED);
        adminRepository.save(user);
        return adminMapper.toUserDto(user);
    }

    public Page<OrderDto> getOrders(Pageable pageable) {
        List<OrderStatus> statusesWithVisibleOperator = List.of(
                OrderStatus.IN_PROGRESS,
                OrderStatus.COMPLETED,
                OrderStatus.CANCELLED
        );
        return adminRepository.findAllOrders(statusesWithVisibleOperator, MatchedOrderStatus.ACCEPTED, pageable);
    }

    public SystemStatsDto getSystemStats() {
        SystemStatsProjection stats = adminRepository.getSystemStatistics();

        return SystemStatsDto.builder()
                .users(SystemStatsDto.UsersStats.builder()
                        .clients(stats.getClientsCount())
                        .operators(stats.getOperatorsCount())
                        .build())
                .orders(SystemStatsDto.OrdersStats.builder()
                        .active(stats.getActiveOrders())
                        .completed(stats.getCompletedOrders())
                        .avgPerOperator(calculateAvgPerOperator(
                                stats.getActiveOrders(),
                                stats.getOperatorsCount()))
                        .build())
                .operators(SystemStatsDto.OperatorsStats.builder()
                        .busy(stats.getBusyOperators())
                        .topOperator(stats.getTopOperatorId() != null
                                ? SystemStatsDto.TopOperator.builder()
                                .operatorId(stats.getTopOperatorId())
                                .completedOrders(stats.getTopOperatorCompletedOrders())
                                .build()
                                : null)
                        .build())
                .reviews(SystemStatsDto.ReviewsStats.builder()
                        .total(stats.getTotalReviews())
                        .build())
                .build();
    }

    private Double calculateAvgPerOperator(Long activeOrders, Long operatorsCount) {
        return operatorsCount > 0
                ? (double) Math.round((activeOrders.doubleValue() / operatorsCount) * 10) / 10
                : 0.0;
    }
}
