package com.sunt.NetworkTester.mapper;

import com.sunt.NetworkTester.DTO.EmailCreateDTO;
import com.sunt.NetworkTester.DTO.EmailResponseDTO;
import com.sunt.NetworkTester.Entity.EmailEntity;

import java.util.UUID;

public class EmailMapper {
    //Create
    
    public static EmailEntity toEntity(EmailCreateDTO dto) {
        if(dto == null) return null;
        
        EmailEntity e = new EmailEntity();
        
        e.setEmail(dto.getEmail());
        
        return e;
    }
    
    //Update
    //Response
    public static EmailResponseDTO toResponse(EmailEntity e) {
        if (e == null) return null;
        
        EmailResponseDTO dto = new EmailResponseDTO();
        
        dto.setId((e.getId().toString()));
        dto.setEmail(e.getEmail());
        
        return dto;
    }
}
