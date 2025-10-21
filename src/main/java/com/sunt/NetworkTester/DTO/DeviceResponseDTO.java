package com.sunt.NetworkTester.DTO;

import lombok.Data;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

@Data
public class DeviceResponseDTO {
    private UUID id;
    private String name;
    private String ip;
    private Integer pingInterval;
    private Boolean testAlways;
    private Integer minOfflineAlarm;
    private List<DayOfWeek> notificationDays;
    private LocalTime startTime;
    private LocalTime endTime;
}

