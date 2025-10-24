package com.sunt.NetworkTester.Service.Email;

import com.sunt.NetworkTester.Entity.DeviceEntity;

public interface EmailSender {
    void sendDeviceOfflineAlert(DeviceEntity e);
}
