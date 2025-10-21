package com.sunt.NetworkTester.DTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCreateDTO {
    @JsonAlias({"Name", "deviceName"})
    private String name;

    @JsonAlias({"IP","Ip"})
    private String ip;

    @NotNull @Min(100) private Integer pingInterval;
    @NotNull private Boolean testAlways;
    @NotNull @Min(0) private Integer minOfflineAlarm;

    private List<DayOfWeek> notificationDays;
    private LocalTime startTime;
    private LocalTime endTime;
}
