package com.example.drones.admin;

import com.example.drones.admin.dto.UserDto;
import com.example.drones.user.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface AdminMapper {

    UserDto toUserDto(UserEntity user);
}
