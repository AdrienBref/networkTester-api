package com.sunt.NetworkTester.Service.Email;

import com.sunt.NetworkTester.Entity.DeviceEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "false")
public class NoopEmailSender implements EmailSender {

    public void send(String to, String subject, String body) {
        // no hace nada
    }

    @Override
    public void sendDeviceOfflineAlert(DeviceEntity e) {
        
    }
}
