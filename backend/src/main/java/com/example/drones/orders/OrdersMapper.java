package com.example.drones.orders;

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
    @Mapping(source = "user.id", target = "clientId")
    OrderResponse toResponse(OrdersEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "service", ignore = true)
    void updateEntityFromRequest(OrderUpdateRequest request, @MappingTarget OrdersEntity entity);
}