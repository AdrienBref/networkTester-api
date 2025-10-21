package com.sunt.NetworkTester.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceUpdateDTO {

    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String ip;

    @Min(100)
    @Max(86_400_000)
    private Integer pingInterval;

    private Boolean testAlways;

    @Min(0)
    private Integer minOfflineAlarm;

    // Días de notificación (pueden ser null si testAlways=true)
    private List<DayOfWeek> notificationDays;

    // Horario (pueden ser null si testAlways=true)
    private LocalTime startTime;
    private LocalTime endTime;
}

