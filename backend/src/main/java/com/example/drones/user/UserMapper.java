package com.example.drones.user;

import com.example.drones.auth.dto.LoginResponse;
import com.example.drones.auth.dto.RegisterRequest;
import com.example.drones.operators.dto.OperatorDto;
import com.example.drones.operators.dto.OperatorPortfolioDto;
import com.example.drones.operators.dto.OperatorProfileDto;
import com.example.drones.user.dto.UserResponse;
import com.example.drones.user.dto.UserUpdateRequest;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    @Mapping(target = "password", source = "hashedPassword")
    @Mapping(target = "role", expression = "java(com.example.drones.user.UserRole.CLIENT)")
    UserEntity toEntity(RegisterRequest registerRequest, String hashedPassword);

    @Mapping(target = "username", source = "displayName")
    UserResponse toResponse(UserEntity userEntity);

    @Mapping(target = "displayName", source = "username")
    @Mapping(target = "role", ignore = true)
    void updateEntityFromRequest(UserUpdateRequest request, @MappingTarget UserEntity entity);

    @Mapping(target = "userId", source = "userEntity.id")
    @Mapping(target = "username", source = "userEntity.displayName")
    @Mapping(target = "token", source = "token")
    LoginResponse toLoginResponse(UserEntity userEntity, String token);

    @Mapping(target = "services", source = "services")
    OperatorProfileDto toOperatorProfileDto(UserEntity userEntity, List<String> services);

    @Mapping(target = "username", source = "user.displayName")
    @Mapping(target = "operatorServices", source = "services")
    @Mapping(target = "portfolio", source = "portfolio")
    OperatorDto toOperatorDto(UserEntity user, List<String> services, OperatorPortfolioDto portfolio);
}