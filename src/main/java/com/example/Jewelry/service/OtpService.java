package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.OtpVerification;
import com.example.Jewelry.repository.OtpVerificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Random;

/**
 * Singleton Service để quản lý OTP
 * Đảm bảo chỉ có duy nhất một instance của class này trong toàn bộ ứng dụng
 */
@Service
@Scope("singleton")
public class OtpService {

    @Autowired
    private OtpVerificationRepository otpRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Tạo và gửi OTP cho email
     */
    @Transactional
    public void sendOtp(String email, String fullName, String username, String passwordHash) {
        // Xóa OTP cũ nếu có
        otpRepository.deleteByEmail(email);

        // Tạo OTP ngẫu nhiên (6 chữ số)
        String otpCode = generateOtp();

        // Tạo entity OTP
        OtpVerification otp = new OtpVerification(email, otpCode, fullName, username, passwordHash);
        otpRepository.save(otp);

        // Gửi email
        emailService.sendOtpEmail(email, otpCode, fullName);
    }

    /**
     * Xác nhận OTP
     */
    @Transactional
    public boolean verifyOtp(String email, String otpCode) {
        Optional<OtpVerification> otpOptional = otpRepository.findByEmailAndOtpCodeAndIsVerifiedFalse(email, otpCode);

        if (otpOptional.isEmpty()) {
            return false;
        }

        OtpVerification otp = otpOptional.get();

        // Kiểm tra OTP có hết hạn không
        if (otp.isExpired()) {
            otpRepository.delete(otp);
            return false;
        }

        // Đánh dấu OTP đã xác nhận
        otp.setIsVerified(true);
        otpRepository.save(otp);

        return true;
    }

    /**
     * Lấy thông tin OTP đã xác nhận
     */
    public Optional<OtpVerification> getVerifiedOtp(String email) {
        Optional<OtpVerification> otpOptional = otpRepository.findByEmail(email);

        if (otpOptional.isEmpty()) {
            return Optional.empty();
        }

        OtpVerification otp = otpOptional.get();

        // Kiểm tra OTP đã xác nhận
        if (otp.getIsVerified() && !otp.isExpired()) {
            return Optional.of(otp);
        }

        return Optional.empty();
    }

    /**
     * Xóa OTP sau khi sử dụng
     */
    @Transactional
    public void deleteOtp(String email) {
        otpRepository.deleteByEmail(email);
    }

    /**
     * Tạo OTP ngẫu nhiên 6 chữ số
     */
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // Tạo số từ 100000 đến 999999
        return String.valueOf(otp);
    }
}
