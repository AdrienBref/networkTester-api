package com.sunt.NetworkTester.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;



public record DeviceStatusDTO(
        UUID id,
        boolean online,
        Long latencyMs,     // null si desconocido
        Instant updatedAt   // última vez que se midió
) {}