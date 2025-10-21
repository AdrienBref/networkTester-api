package com.sunt.NetworkTester.DTO;

public record DeviceRealtimeDTO(
        java.util.UUID id,
        boolean online,
        Long latencyMs,
        java.time.Instant updatedAt
) {}
