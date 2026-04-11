package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.Account;

import java.util.List;
import java.util.Optional;

public interface AccountService {

    Optional<Account> findById(Integer id);

    List<Account> findAll();

    Account save(Account entity);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    long count();

    boolean login(String username, String password);
}
