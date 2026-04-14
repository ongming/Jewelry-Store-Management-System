package com.example.Jewelry.repository;

import com.example.Jewelry.model.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findByEmailAndOtpCodeAndIsVerifiedFalse(String email, String otpCode);
    Optional<OtpVerification> findByEmail(String email);
    void deleteByEmail(String email);
}
