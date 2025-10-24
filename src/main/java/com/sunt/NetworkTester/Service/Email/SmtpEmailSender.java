package com.sunt.NetworkTester.Service.Email;

import com.sunt.NetworkTester.DTO.EmailCreateDTO;
import com.sunt.NetworkTester.DTO.EmailResponseDTO;
import com.sunt.NetworkTester.Entity.DeviceEntity;
import com.sunt.NetworkTester.Entity.EmailEntity;
import com.sunt.NetworkTester.Repository.EmailRepository;
import com.sunt.NetworkTester.mapper.EmailMapper;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SmtpEmailSender implements EmailSender {

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.properties.mail.smtp.auth}")
    private boolean auth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    private boolean starttls;
    
    private final EmailRepository emailRepository;

    public SmtpEmailSender(EmailRepository emailRepository) {
        this.emailRepository = emailRepository;
    }

    @Override
    public void sendDeviceOfflineAlert(DeviceEntity d) {
        System.out.println("\u001B[33m⚠️  [Enviando Email...] \u001B[0m");
             // tu dirección de Gmail
        ; // ponla en variable de entorno

        Properties props = new Properties();
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        // Si prefieres 465 (SSL puro), usa:
        // props.put("mail.smtp.ssl.enable", "true");
        // props.put("mail.smtp.port", "465");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        Message msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(username, "Argos NetworkTester"));
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        try {
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse("developadri@gmail.com"));
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        try {
            msg.setSubject("⚠️  [ALERTA] El dispositivo: " + d.getName() + " con ip: " + d.getIp() + " lleva desconectado " + d.getMinOfflineAlarm() + " minutos.");
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        try {
            msg.setText("⚠️  [ALERTA] El dispositivo: " + d.getName() + " con ip: " + d.getIp() + " lleva desconectado " + d.getMinOfflineAlarm() + " minutos.");
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

        try {
            Transport.send(msg);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Enviado ✅");
    }
    
    public List<EmailResponseDTO> findAll() {
        
        List<EmailEntity> eList = emailRepository.findAll();
        List<EmailResponseDTO> rDtoList = new ArrayList<>();
        
        for(EmailEntity e : eList) {
            rDtoList.add(EmailMapper.toResponse(e));
        }
        
        return rDtoList;
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

