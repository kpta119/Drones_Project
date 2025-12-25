package com.example.drones.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    boolean existsByEmail(String email);

    Optional<UserEntity> findByEmail(String email);

    @Query("""
                    SELECT u FROM UserEntity u
                    LEFT JOIN FETCH u.portfolio portfolio
                    LEFT JOIN FETCH portfolio.photos photo
                    WHERE u.id = :userId
            """)
    Optional<UserEntity> findByIdWithPortfolio(UUID userId);
}
