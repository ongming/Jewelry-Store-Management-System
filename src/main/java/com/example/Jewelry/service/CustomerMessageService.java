package com.example.Jewelry.service;

import org.springframework.stereotype.Service;

@Service
public class CustomerMessageService {

    public void sendSms(String phone, String customerName, String message) {
        if (phone == null || phone.trim().isEmpty()) {
            return;
        }

        System.out.println(
            "[WEEKLY_DIGEST_SMS] to=" + phone
                + " | customer=" + customerName
                + "\n" + message
        );
    }
}

