package com.example.Jewelry.service.impl;

import com.example.Jewelry.model.entity.Account;
import com.example.Jewelry.repository.AccountRepository;
import com.example.Jewelry.service.AccountService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public Optional<Account> findById(Integer id) {
        return accountRepository.findById(id);
    }

    @Override
    public Optional<Account> findByUsername(String username) {
        return accountRepository.findByUsername(username);
    }

    @Override
    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    @Override
    public Account save(Account entity) {
        return accountRepository.save(entity);
    }

    @Override
    public void deleteById(Integer id) {
        accountRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return accountRepository.existsById(id);
    }

    @Override
    public long count() {
        return accountRepository.count();
    }

    @Override
    public Optional<Account> login(String username, String password) {
        return accountRepository.findByUsernameAndPasswordHashAndStatusIgnoreCase(username, password, "ACTIVE");
    }
}
