package com.sunt.NetworkTester.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

// Entity Runtime
@Entity
@Table(name = "device_status")
@Data
public class DeviceRuntimeStatus {
    @Id
    private UUID deviceId; // mismo id que DeviceEntity (PK=FK)

    private Boolean lastState;            // true=online, false=offline, null=desconocido
    private Instant offlineSince;         // null si online
    private Boolean alarmSent;            // null/false por defecto
    private Instant lastAlarmAt;

    
}
