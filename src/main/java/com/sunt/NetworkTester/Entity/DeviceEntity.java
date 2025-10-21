package com.sunt.NetworkTester.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "device_entity")
public class DeviceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String ip;

    /** Intervalo de ping en milisegundos */
    @Column(name = "ping_interval", nullable = false)
    private int pingInterval;

    /** Si se testea siempre (ignora horario y días) */
    @Column(name = "test_always", nullable = false)
    private boolean testAlways;

    /** Minutos que debe estar offline antes de lanzar alarma */
    @Column(name = "min_offline_alarm", nullable = false)
    private int minOfflineAlarm;

    /** Horario general (inicio y fin) */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    @Column(name = "start_time")
    private LocalTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    @Column(name = "end_time")
    private LocalTime endTime;

    /** Días permitidos para notificación (L M X J V S D) */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "device_notify_day", joinColumns = @JoinColumn(name = "device_id"))
    @Column(name = "day_of_week", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> notifyDays = new HashSet<>();

    @OneToMany(mappedBy = "device",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("dayOfWeek, startTime")
    private java.util.List<DeviceScheduleRule> rules = new java.util.ArrayList<>();
    
}
