package com.example.drones.orders;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NewMatchedOrdersRepository extends JpaRepository<NewMatchedOrderEntity, Integer> {
}
