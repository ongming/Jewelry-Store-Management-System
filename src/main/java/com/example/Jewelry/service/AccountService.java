package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.Account;

import java.util.List;
import java.util.Optional;

public interface AccountService {

    Optional<Account> findById(Integer id);

    Optional<Account> findByUsername(String username);

    List<Account> findAll();

    Account save(Account entity);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    long count();

    Optional<Account> login(String username, String password);

    List<Account> findByStatus(String status);

    void suspendAccount(Integer accountId);

    void activateAccount(Integer accountId);

    void lockAccount(Integer accountId);

    /**
     * Lấy tất cả tài khoản INACTIVE (bất kể role)
     */
    List<Account> findAllInactiveAccounts();
}