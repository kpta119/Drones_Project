package com.example.drones.user;

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

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "user_role", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private UserRole role = UserRole.CLIENT;

    @Column(name = "username", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    @Email
    private String email;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "google_user_id")
    private String googleUserId;

    @Column(name = "google_access_token")
    private String googleAccessToken;

    @Column(name = "coordinates")
    private String coordinates;

    @Column(name = "radius")
    private Integer radius;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "certificates")
    private List<String> certificates;

}