// DeviceScheduleRule.java
package com.sunt.NetworkTester.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "device_schedule_rule")
public class DeviceScheduleRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ⚠️ No uses "day" como nombre de columna: H2 lo confunde.
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 16)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    @JsonIgnore            // ← evita que Jackson siga volviendo al Device
    private DeviceEntity device;
}
