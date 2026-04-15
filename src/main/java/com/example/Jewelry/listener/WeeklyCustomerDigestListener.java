package com.example.Jewelry.listener;

import com.example.Jewelry.event.WeeklyCustomerDigestEvent;
import com.example.Jewelry.service.CustomerMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class WeeklyCustomerDigestListener {

    private final CustomerMessageService customerMessageService;
    private final JavaMailSender mailSender;

    @Value("${weekly.digest.notify-admin-email:false}")
    private boolean notifyAdminEmail;

    @Value("${weekly.digest.admin-email:}")
    private String adminEmail;

    public WeeklyCustomerDigestListener(CustomerMessageService customerMessageService,
                                        JavaMailSender mailSender) {
        this.customerMessageService = customerMessageService;
        this.mailSender = mailSender;
    }

    @EventListener
    public void handle(WeeklyCustomerDigestEvent event) {
        int sentCount = 0;

        for (WeeklyCustomerDigestEvent.CustomerDigestMessage customerMessage : event.getCustomerMessages()) {
            String phone = customerMessage.customerPhone();
            if (phone == null || phone.trim().isEmpty()) {
                continue;
            }

            customerMessageService.sendSms(phone, customerMessage.customerName(), customerMessage.message());
            sentCount++;
        }

        if (notifyAdminEmail && adminEmail != null && !adminEmail.isBlank()) {
            String subject = "[Weekly Digest] Tong ket gui khach hang";
            String body = "Da gui " + sentCount + " tin nhan tong ket tuan.\n"
                + "San pham ban chay nhat: " + event.getTopProductName()
                + " (" + event.getTopProductQuantity() + ")\n"
                + "Tuan: " + event.getWeekStart() + " -> " + event.getWeekEnd();

            SimpleMailMessage adminMessage = new SimpleMailMessage();
            adminMessage.setFrom("noreply@jewelrystore.com");
            adminMessage.setTo(adminEmail.trim());
            adminMessage.setSubject(subject);
            adminMessage.setText(body);

            try {
                mailSender.send(adminMessage);
            } catch (Exception ex) {
                System.err.println("Loi gui email tong ket admin: " + ex.getMessage());
            }
        }
    }
}
