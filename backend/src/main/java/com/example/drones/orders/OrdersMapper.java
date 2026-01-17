package com.example.drones.orders;

import com.example.drones.calendar.dto.SchedulableOrders;
import com.example.drones.operators.dto.MatchedOrderDto;
import com.example.drones.orders.dto.OrderRequest;
import com.example.drones.orders.dto.OrderResponse;
import com.example.drones.orders.dto.OrderUpdateRequest;
import com.example.drones.services.ServicesEntity;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        imports = {OrderStatus.class}
)
public interface OrdersMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "service", source = "serviceEntity")
    @Mapping(target = "parameters", source = "request.parameters")
    @Mapping(target = "status", expression = "java(OrderStatus.OPEN)")
    OrdersEntity toEntity(OrderRequest request, ServicesEntity serviceEntity);

    @Mapping(target = "service", source = "service.name")
    @Mapping(source = "user.id", target = "client_id")
    OrderResponse toResponse(OrdersEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "service", ignore = true)
    void updateEntityFromRequest(OrderUpdateRequest request, @MappingTarget OrdersEntity entity);

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "clientId", source = "entity.user.id")
    @Mapping(target = "title", source = "entity.title")
    @Mapping(target = "description", source = "entity.description")
    @Mapping(target = "service", source = "entity.service.name")
    @Mapping(target = "parameters", source = "entity.parameters")
    @Mapping(target = "coordinates", source = "entity.coordinates")
    @Mapping(target = "fromDate", source = "entity.fromDate")
    @Mapping(target = "toDate", source = "entity.toDate")
    @Mapping(target = "createdAt", source = "entity.createdAt")
    @Mapping(target = "orderStatus", source = "entity.status")
    @Mapping(target = "distance", source = "distance")
    @Mapping(target = "clientStatus", source = "matchedOrder.clientStatus")
    @Mapping(target = "operatorStatus", source = "matchedOrder.operatorStatus")
    MatchedOrderDto toMatchedOrderDto(OrdersEntity entity, NewMatchedOrderEntity matchedOrder, Double distance);

    @Mapping(target = "alreadyAdded", ignore = true)
    @Mapping(target = "service", source = "ordersEntity.service.name")
    @Mapping(target = "clientId", source = "ordersEntity.userId")
    SchedulableOrders toSchedulableOrders(OrdersEntity ordersEntity);
}