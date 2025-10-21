package com.sunt.NetworkTester.Controller;

import com.sunt.NetworkTester.DTO.DeviceStatusDTO;
import com.sunt.NetworkTester.Entity.DeviceEntity;
import com.sunt.NetworkTester.DTO.ReqDeviceConnTest;
import com.sunt.NetworkTester.Service.DeviceStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // permite peticiones desde tu front local
public class DeviceController {

    private final DeviceStatusService service;

    @GetMapping
    public List<DeviceEntity> getAll() {
        return service.findAll();
    }
    
    @PutMapping("/{id}")
    public boolean modifyDeviceParameters(@PathVariable String id, @RequestBody DeviceEntity device) {
        System.out.println(device.toString());
        return false;
    }
    

}
