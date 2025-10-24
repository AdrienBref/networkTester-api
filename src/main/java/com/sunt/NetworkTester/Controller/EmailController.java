package com.sunt.NetworkTester.Controller;

import com.sunt.NetworkTester.DTO.EmailCreateDTO;
import com.sunt.NetworkTester.DTO.EmailResponseDTO;
import com.sunt.NetworkTester.Service.Email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/email/recipients")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // permite peticiones desde tu front local
public class EmailController {
    
    private final EmailService emailService;
    
    @GetMapping
    public ResponseEntity<List<EmailResponseDTO>> getAll(){
        return ResponseEntity.ok(emailService.findAll());
    }

    @PutMapping()
    public ResponseEntity<List<EmailResponseDTO>> update(@RequestBody List<EmailCreateDTO> dtoList) {
        List<EmailResponseDTO> response = emailService.saveList(dtoList);
        return ResponseEntity.ok(response);
    }
    
}
