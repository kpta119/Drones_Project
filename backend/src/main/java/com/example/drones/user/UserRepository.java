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
            SELECT u FROM UserEntity u
            LEFT JOIN FETCH u.portfolio
            JOIN OperatorServicesEntity os ON u.id = os.operatorId
            WHERE u.role = 'OPERATOR'
              AND u.id != :creatorId
              AND os.serviceName = :serviceName
              AND u.coordinates IS NOT NULL
              AND u.radius IS NOT NULL
              AND (
                  6371 * acos(
                      cos(radians(:orderLat)) * cos(radians(CAST(FUNCTION('SPLIT_PART', u.coordinates, ',', 1) AS double))) *
                      cos(radians(CAST(FUNCTION('SPLIT_PART', u.coordinates, ',', 2) AS double)) - radians(:orderLon)) +
                      sin(radians(:orderLat)) * sin(radians(CAST(FUNCTION('SPLIT_PART', u.coordinates, ',', 1) AS double)))
                  )
              ) <= u.radius
            """)
    List<UserEntity> findMatchingOperators(
            @Param("serviceName") String serviceName,
            @Param("orderLat") double orderLat,
            @Param("orderLon") double orderLon,
            @Param("creatorId") UUID creatorId
    );

    Optional<UserEntity> findByProviderUserId(String providerUserId);
}
