package com.example.drones.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemStatsDto {
    private UsersStats users;
    private OrdersStats orders;
    private OperatorsStats operators;
    private ReviewsStats reviews;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UsersStats {
        private Long clients;
        private Long operators;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrdersStats {
        private Long active;
        private Long completed;
        private Double avgPerOperator;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OperatorsStats {
        private Long busy;
        private TopOperator topOperator;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopOperator {
        private UUID operatorId;
        private Long completedOrders;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewsStats {
        private Long total;
    }
}

