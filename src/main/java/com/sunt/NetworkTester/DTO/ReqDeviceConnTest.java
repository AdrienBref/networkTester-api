package com.sunt.NetworkTester.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReqDeviceConnTest  {
    private UUID id;
    private boolean isOnline;
}
