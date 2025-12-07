package com.example.drones.user;

import com.example.drones.auth.dto.LoginResponse;
import com.example.drones.auth.dto.RegisterRequest;
import com.example.drones.operators.dto.OperatorProfileDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    @Mapping(target = "password", source = "hashedPassword")
    @Mapping(target = "role", expression = "java(com.example.drones.user.UserRole.CLIENT)")
    UserEntity toEntity(RegisterRequest registerRequest, String hashedPassword);

    @Mapping(target = "userId", source = "userEntity.id")
    @Mapping(target = "username", source = "userEntity.displayName")
    @Mapping(target = "token", source = "token")
    LoginResponse toLoginResponse(UserEntity userEntity, String token);

    @Mapping(target = "services", source = "services")
    OperatorProfileDto toOperatorProfileDto(UserEntity userEntity, List<String> services);
}