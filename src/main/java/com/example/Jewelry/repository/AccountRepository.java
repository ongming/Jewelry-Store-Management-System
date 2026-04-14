package com.example.Jewelry.repository;

import com.example.Jewelry.model.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {

    Optional<Account> findByUsernameAndPasswordHashAndStatusIgnoreCase(String username, String passwordHash, String status);

    Optional<Account> findByUsername(String username);

    /**
     * Lấy tất cả tài khoản INACTIVE (bất kể role là gì: Staff hay Admin)
     * Sử dụng DISTINCT để tránh duplicate khi join với các bảng con
     */
    List<Account> findByStatusIgnoreCase(String status);
}
