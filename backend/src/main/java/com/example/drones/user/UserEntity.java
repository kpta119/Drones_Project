package com.example.drones.user;

import com.example.drones.operators.PortfolioEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "role", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private UserRole role = UserRole.CLIENT;

    @Column(name = "username")
    private String displayName;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column
    private String password;

    @Column(nullable = false, unique = true)
    @Email
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "provider_user_id", unique = true)
    private String providerUserId;

    @Column(name = "provider_refresh_token")
    private String providerRefreshToken;

    @Column(name = "coordinates")
    private String coordinates;

    @Column(name = "radius")
    private Integer radius;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "certificates")
    private List<String> certificates;

    @OneToOne(mappedBy = "operator")
    private PortfolioEntity portfolio;
}