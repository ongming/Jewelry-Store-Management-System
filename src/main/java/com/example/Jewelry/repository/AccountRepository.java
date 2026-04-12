package com.example.Jewelry.repository;

import com.example.Jewelry.model.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {

    Optional<Account> findByUsernameAndPasswordHashAndStatusIgnoreCase(String username, String passwordHash, String status);

    Optional<Account> findByUsername(String username);
}
