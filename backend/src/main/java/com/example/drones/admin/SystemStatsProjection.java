package com.example.drones.admin;

import java.util.UUID;

public interface SystemStatsProjection {
    Long getClientsCount();

    Long getOperatorsCount();

    Long getActiveOrders();

    Long getCompletedOrders();

    Long getBusyOperators();

    UUID getTopOperatorId();

    Long getTopOperatorCompletedOrders();

    Long getTotalReviews();
}
