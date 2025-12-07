package com.example.drones.services;

import com.example.drones.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface OperatorServicesRepository extends JpaRepository<OperatorServicesEntity, Integer> {
    void deleteAllByOperator(UserEntity operator);

    List<OperatorServicesEntity> findAllByOperator(UserEntity operator);

}
