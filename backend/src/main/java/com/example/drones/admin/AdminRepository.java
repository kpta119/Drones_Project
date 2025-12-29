package com.example.drones.admin;

import com.example.drones.admin.dto.UserDto;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface AdminRepository extends JpaRepository<UserEntity, UUID> {

    @Query("""
                SELECT new com.example.drones.admin.dto.UserDto(
                    CAST(u.id AS string),
                    u.displayName,
                    u.role,
                    u.name,
                    u.surname,
                    u.email,
                    u.phoneNumber
                )
                FROM UserEntity u
                WHERE (:query IS NULL OR
                       u.displayName ILIKE %:query% OR
                       u.email ILIKE %:query%)
                  AND (CAST(:role AS string) IS NULL OR u.role = :role)
            """)
    Page<UserDto> findAllByQueryAndRole(@Param("query") String query, @Param("role") UserRole role, Pageable pageable);
}
