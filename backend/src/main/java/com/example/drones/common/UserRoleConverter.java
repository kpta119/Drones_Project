package com.example.drones.common;

import com.example.drones.user.UserRole;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class UserRoleConverter implements Converter<String, UserRole> {

    @Override
    public UserRole convert(String value) {
        return UserRole.valueOf(value.toUpperCase());
    }
}
