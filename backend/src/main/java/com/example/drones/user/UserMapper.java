package com.example.drones.user;

import com.example.drones.auth.dto.RegisterRequest;
import com.example.drones.user.dto.UserResponse;
import com.example.drones.user.dto.UserUpdateRequest;
import org.mapstruct.*;

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
    void updateEntityFromRequest(UserUpdateRequest request, @MappingTarget UserEntity entity);
}