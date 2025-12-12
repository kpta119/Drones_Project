package com.example.drones.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServicesService {

    private final ServicesRepository servicesRepository;

    @Cacheable("services")
    public List<String> getAllServices() {
        return servicesRepository.findAll()
                .stream()
                .map(ServicesEntity::getName)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "services", allEntries = true)
    public List<String> addServices(List<String> serviceNames) {
        List<ServicesEntity> newEntities = serviceNames.stream()
                .filter(name -> !servicesRepository.existsById(name))
                .map(ServicesEntity::new)
                .collect(Collectors.toList());

        servicesRepository.saveAll(newEntities);

        return serviceNames;
    }
}