package com.sunt.NetworkTester.Controller;

import com.sunt.NetworkTester.DTO.DeviceCreateDTO;
import com.sunt.NetworkTester.DTO.DeviceResponseDTO;
import com.sunt.NetworkTester.DTO.DeviceUpdateDTO;
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
    public ResponseEntity<DeviceResponseDTO> updateDevice(@PathVariable String id, @RequestBody DeviceUpdateDTO dto) {
        return ResponseEntity.ok(service.updateDevice(id, dto));
    }
    
    @DeleteMapping("/deleteDevice/{id}")
    public ResponseEntity<DeviceResponseDTO> deleteDevice(@PathVariable String id) {
        return ResponseEntity.ok(service.deleteDevice(id));
    }
    

}
