package com.example.drones.services;

import com.example.drones.user.UserEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "operator_service",
        uniqueConstraints = @UniqueConstraint(columnNames = {"service_name", "operator_id"}))
@Data
@NoArgsConstructor
public class OperatorServicesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "operator_id", insertable = false, updatable = false)
    private UUID operatorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_name", referencedColumnName = "name", insertable = false, updatable = false)
    private ServicesEntity service;

    @ManyToOne
    @JoinColumn(name = "operator_id", referencedColumnName = "id", nullable = false)
    private UserEntity operator;
}
