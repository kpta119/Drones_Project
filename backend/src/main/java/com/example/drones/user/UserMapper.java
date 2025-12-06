package com.example.drones.user;

import com.example.drones.auth.dto.LoginResponse;
import com.example.drones.auth.dto.RegisterRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    @Mapping(target = "password", source = "hashedPassword")
    @Mapping(target = "role", expression = "java(com.example.drones.user.UserRole.CLIENT)")
    UserEntity toEntity(RegisterRequest registerRequest, String hashedPassword);

    @Mapping(target = "userId", source = "userEntity.id")
    @Mapping(target = "email", source = "userEntity.email")
    @Mapping(target = "username", source = "userEntity.displayName")
    @Mapping(target = "role", source = "userEntity.role")
    @Mapping(target = "token", source = "token")
    LoginResponse toLoginResponse(UserEntity userEntity, String token);
}