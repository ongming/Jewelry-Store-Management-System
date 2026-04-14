package com.example.Jewelry.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.example.Jewelry.model.state.AccountState;
import com.example.Jewelry.model.state.ActiveState;
import com.example.Jewelry.model.state.InactiveState;
import com.example.Jewelry.model.state.LockedOutState;
import com.example.Jewelry.model.state.SuspendedState;

@Entity
@Table(name = "account")
@Inheritance(strategy = InheritanceType.JOINED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private int accountId;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "role_name", nullable = false, length = 30)
    private String roleName;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Transient
    private AccountState state;

    public Account() {
        initState();
    }

    public Account(int accountId, String username, String passwordHash, String fullName, String roleName) {
        this.accountId = accountId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.roleName = roleName;
        this.status = "ACTIVE";
        initState();
    }

    public Account(int accountId, String username, String passwordHash, String fullName, String roleName, String status) {
        this.accountId = accountId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.roleName = roleName;
        this.status = status != null ? status : "ACTIVE";
        initState();
    }
    
    @PostLoad
    private void initState() {
        if ("LOCKED".equalsIgnoreCase(this.status)) {
            this.state = new LockedOutState();
        } else if ("SUSPENDED".equalsIgnoreCase(this.status)) {
            this.state = new SuspendedState();
        } else if ("INACTIVE".equalsIgnoreCase(this.status)) {
            this.state = new InactiveState();
        } else {
            this.state = new ActiveState();
        }
    }

    public AccountState getState() {
        return state;
    }

    public void setState(AccountState state) {
        this.state = state;
        this.status = state.getStateName();
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        initState(); // Cập nhật lại đối tượng state khi đổi status
    }

    public boolean login() {
        if (state != null) {
            return state.canLogin();
        }
        return false;
    }
    
    public boolean canLogin() {
        if (state != null) {
            return state.canLogin();
        }
        return false;
    }
    
    public boolean canAccessSystem() {
        if (state != null) {
            return state.canAccessSystem();
        }
        return false;
    }
    
    public boolean canModifyData() {
        if (state != null) {
            return state.canModifyData();
        }
        return false;
    }
    
    public String getAccountStateName() {
        if (state != null) {
            return state.getStateName();
        }
        return this.status;
    }
    
    public void suspend() {
        if (state != null) {
            state.suspend(this);
        }
    }

    public void activate() {
        if (state != null) {
            state.activate(this);
        }
    }

    public void lock() {
        if (state != null) {
            state.lock(this);
        }
    }
}