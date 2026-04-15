package com.example.Jewelry.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Singleton Service để gửi email
 * Đảm bảo chỉ có duy nhất một instance của class này trong toàn bộ ứng dụng
 * Spring tự động quản lý lifecycle thông qua @Service + @Scope("singleton")
 */
@Service
@Scope("singleton")
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Gửi email OTP đến người dùng
     */
    public void sendOtpEmail(String toEmail, String otpCode, String fullName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@jewelrystore.com");
            message.setTo(toEmail);
            message.setSubject("🔐 Xác nhận email - Hệ thống quản lý trang sức");
            
            String emailBody = String.format(
                "Xin chào %s,\n\n" +
                "Bạn đã yêu cầu đăng ký tài khoản trên hệ thống quản lý trang sức.\n\n" +
                "Mã OTP xác nhận của bạn là: %s\n\n" +
                "⏰ Mã này sẽ hết hạn trong 15 phút.\n\n" +
                "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ quản lý trang sức",
                fullName,
                otpCode
            );
            
            message.setText(emailBody);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Lỗi gửi email: " + e.getMessage());
            throw new RuntimeException("Không thể gửi email OTP");
        }
    }

    /**
     * Gửi email xác nhận đăng ký thành công
     */
    public void sendRegistrationConfirmation(String toEmail, String fullName, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@jewelrystore.com");
            message.setTo(toEmail);
            message.setSubject("✅ Đăng ký thành công - Hệ thống quản lý trang sức");
            
            String emailBody = String.format(
                "Xin chào %s,\n\n" +
                "Chúc mừng! Tài khoản của bạn đã được xác nhận thành công.\n\n" +
                "Thông tin đăng nhập của bạn:\n" +
                "- Tên đăng nhập: %s\n" +
                "- Địa chỉ: http://localhost:8080/auth/login\n\n" +
                "Bạn có thể sử dụng tên đăng nhập và mật khẩu vừa tạo để đăng nhập vào hệ thống.\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ quản lý trang sức",
                fullName,
                username
            );
            
            message.setText(emailBody);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Lỗi gửi email xác nhận: " + e.getMessage());
        }
    }

    /**
     * Gửi email văn bản tổng quát
     */
    public void sendPlainTextEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@jewelrystore.com");
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Lỗi gửi email thông báo: " + e.getMessage());
        }
    }
}
