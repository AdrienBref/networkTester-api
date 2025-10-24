package com.sunt.NetworkTester.Service.Email;

import com.sunt.NetworkTester.DTO.EmailCreateDTO;
import com.sunt.NetworkTester.DTO.EmailResponseDTO;
import com.sunt.NetworkTester.Entity.EmailEntity;
import com.sunt.NetworkTester.Repository.EmailRepository;
import com.sunt.NetworkTester.mapper.EmailMapper;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class EmailService {

    private final EmailRepository emailRepository;

    public EmailService(EmailRepository emailRepository) {
        this.emailRepository = emailRepository;
    }

    public List<EmailResponseDTO> findAll() {

        List<EmailEntity> eList = emailRepository.findAll();
        List<EmailResponseDTO> rDtoList = new ArrayList<>();

        for(EmailEntity e : eList) {
            rDtoList.add(EmailMapper.toResponse(e));
        }

        return rDtoList;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCache() {
        //AtomicReference<List<EmailResponseDTO>> rDtoList = findAll();
    }
    
    

    @Transactional
    public List<EmailResponseDTO> saveList(List<EmailCreateDTO> dtoList) {
        // 1) Normaliza y deduplica la entrada
        Set<String> incoming = (dtoList == null ? List.<EmailCreateDTO>of() : dtoList).stream()
                .map(EmailCreateDTO::getEmail)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)      // opcional pero recomendable
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 2) Carga existentes (normalizados)
        Set<String> existing = emailRepository.findAllEmails().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // 3) Calcula diffs
        Set<String> toInsert = new HashSet<>(incoming);
        toInsert.removeAll(existing);

        Set<String> toDelete = new HashSet<>(existing);
        toDelete.removeAll(incoming);

        // 4) Aplica cambios
        if (!toDelete.isEmpty()) {
            emailRepository.deleteAllByEmailIn(toDelete);
        }

        if (!toInsert.isEmpty()) {
            List<EmailEntity> newEntities = toInsert.stream()
                    .map(e -> EmailEntity.builder().email(e).build())
                    .toList();
            emailRepository.saveAll(newEntities);
        }

        // 5) Devuelve estado final como ResponseDTO
        return emailRepository.findAll().stream()
                .map(e -> new EmailResponseDTO(e.getId().toString(), e.getEmail()))
                .toList();
    }
}


