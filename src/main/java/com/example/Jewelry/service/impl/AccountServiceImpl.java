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
        // Tìm tài khoản theo username và mật khẩu
        return accountRepository.findByUsername(username)
                .filter(acc -> acc.getPasswordHash().equals(password));
    }

    @Override
    public List<Account> findByStatus(String status) {
        return accountRepository.findAll().stream()
                .filter(a -> status.equalsIgnoreCase(a.getStatus()))
                .toList();
    }

    @Override
    public void suspendAccount(Integer accountId) {
        accountRepository.findById(accountId).ifPresent(account -> {
            account.suspend();
            accountRepository.save(account);
        });
    }

    @Override
    public void activateAccount(Integer accountId) {
        accountRepository.findById(accountId).ifPresent(account -> {
            account.activate();
            accountRepository.save(account);
        });
    }

    @Override
    public void lockAccount(Integer accountId) {
        accountRepository.findById(accountId).ifPresent(account -> {
            account.lock();
            accountRepository.save(account);
        });
    }

    @Override
    public List<Account> findAllInactiveAccounts() {
        return accountRepository.findByStatusIgnoreCase("INACTIVE");
    }
}