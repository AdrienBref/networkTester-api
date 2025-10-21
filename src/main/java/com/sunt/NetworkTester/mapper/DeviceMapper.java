package com.sunt.NetworkTester.mapper;

import com.sunt.NetworkTester.DTO.DeviceCreateDTO;
import com.sunt.NetworkTester.DTO.DeviceResponseDTO;
import com.sunt.NetworkTester.DTO.DeviceUpdateDTO;
import com.sunt.NetworkTester.Entity.DeviceEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class DeviceMapper {

    // === CREATE ===
    public static DeviceEntity toEntity(DeviceCreateDTO dto) {
        if (dto == null) return null;

        DeviceEntity e = new DeviceEntity();
        e.setName(dto.getName());
        e.setIp(dto.getIp());
        e.setPingInterval(dto.getPingInterval());
        e.setTestAlways(dto.getTestAlways());
        e.setMinOfflineAlarm(dto.getMinOfflineAlarm());

        // Convertimos lista de días a Set (si procede)
        if (dto.getNotificationDays() != null) {
            e.setNotifyDays(new HashSet<>(dto.getNotificationDays()));
        }

        e.setStartTime(dto.getStartTime());
        e.setEndTime(dto.getEndTime());

        // Limpieza por lógica de negocio
        if (Boolean.TRUE.equals(e.isTestAlways())) {
            e.setNotifyDays(null);
            e.setStartTime(null);
            e.setEndTime(null);
        }

        return e;
    }

    // === UPDATE ===
    public static void apply(DeviceUpdateDTO dto, DeviceEntity e) {
        if (dto == null || e == null) return;

        if (dto.getName() != null) e.setName(dto.getName());
        if (dto.getIp() != null) e.setIp(dto.getIp());
        if (dto.getPingInterval() != null) e.setPingInterval(dto.getPingInterval());
        if (dto.getTestAlways() != null) e.setTestAlways(dto.getTestAlways());
        if (dto.getMinOfflineAlarm() != null) e.setMinOfflineAlarm(dto.getMinOfflineAlarm());
        if (dto.getNotificationDays() != null) e.setNotifyDays(new HashSet<>(dto.getNotificationDays()));
        if (dto.getStartTime() != null) e.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null) e.setEndTime(dto.getEndTime());

        // Limpieza por lógica de negocio
        if (Boolean.TRUE.equals(e.isTestAlways())) {
            e.setNotifyDays(null);
            e.setStartTime(null);
            e.setEndTime(null);
        }
    }

    // === RESPONSE ===
    public static DeviceResponseDTO toResponse(DeviceEntity e) {
        if (e == null) return null;

        DeviceResponseDTO r = new DeviceResponseDTO();
        r.setId(e.getId());
        r.setName(e.getName());
        r.setIp(e.getIp());
        r.setPingInterval(e.getPingInterval());
        r.setTestAlways(e.isTestAlways());
        r.setMinOfflineAlarm(e.getMinOfflineAlarm());

        if (e.getNotifyDays() != null) {
            r.setNotificationDays(e.getNotifyDays().stream().toList());
        }

        r.setStartTime(e.getStartTime());
        r.setEndTime(e.getEndTime());
        return r;
    }
}
