package com.sunt.NetworkTester.Repository;

import com.sunt.NetworkTester.Entity.DeviceRuntimeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeviceRuntimeStatusRepository extends JpaRepository<DeviceRuntimeStatus, UUID> {}