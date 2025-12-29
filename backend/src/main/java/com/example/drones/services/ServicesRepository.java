package com.example.drones.services;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ServicesRepository extends JpaRepository<ServicesEntity, String> {

    @Query("SELECT os.service FROM OperatorServicesEntity os WHERE os.operator.id = :operatorId")
    List<ServicesEntity> findAllByOperatorId(@Param("operatorId") UUID operatorId);
}

