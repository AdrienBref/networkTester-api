package com.sunt.NetworkTester.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResDeviceConnTest {
    private UUID id;
    private boolean isOnline;
}
