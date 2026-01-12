package com.sunt.NetworkTester.Repository;

import com.sunt.NetworkTester.Entity.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID> {
    List<DeviceEntity> findAll();
    DeviceEntity getById(UUID id);
    
    Optional<DeviceEntity> findById(UUID id);
}
