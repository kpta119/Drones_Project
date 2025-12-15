package com.example.drones.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    @Query(value = """
        SELECT u.* FROM users u
        JOIN operator_service os ON u.id = os.operator_id
        WHERE u.role = 'OPERATOR'
          AND os.service_name = :serviceName
          AND u.coordinates IS NOT NULL
          AND u.radius IS NOT NULL
          AND (
              6371 * acos(
                  cos(radians(:orderLat)) * cos(radians(CAST(SPLIT_PART(u.coordinates, ',', 1) AS DOUBLE PRECISION))) *
                  cos(radians(CAST(SPLIT_PART(u.coordinates, ',', 2) AS DOUBLE PRECISION)) - radians(:orderLon)) +
                  sin(radians(:orderLat)) * sin(radians(CAST(SPLIT_PART(u.coordinates, ',', 1) AS DOUBLE PRECISION)))
              )
          ) <= u.radius
        """, nativeQuery = true)
    List<UserEntity> findMatchingOperators(
            @Param("serviceName") String serviceName,
            @Param("orderLat") double orderLat,
            @Param("orderLon") double orderLon
    );
}
