package com.example.Jewelry.repository;

import com.example.Jewelry.model.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {

    Optional<Account> findByUsernameAndPasswordHash(String username, String passwordHash);

    Optional<Account> findByUsername(String username);
}
