package com.sunt.NetworkTester.Controller;

import com.sunt.NetworkTester.DTO.DeviceCreateDTO;
import com.sunt.NetworkTester.DTO.DeviceResponseDTO;
import com.sunt.NetworkTester.Entity.DeviceEntity;
import com.sunt.NetworkTester.Service.DeviceStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    
    @PostMapping("/createDevice")
    public ResponseEntity<DeviceResponseDTO> createDevice(@Valid @RequestBody DeviceCreateDTO dto) {
        return ResponseEntity.ok(service.createDevice(dto));
    }
    
    @PutMapping("/{id}")
    public boolean updateDevice(@PathVariable String id, @RequestBody DeviceEntity device) {
        System.out.println(device.toString());
        return false;
    }
    

}
