package com.example.drones.user;

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
}