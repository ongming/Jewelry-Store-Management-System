package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.Account;
import com.example.Jewelry.model.state.ActiveState;
import com.example.Jewelry.model.state.SuspendedState;
import com.example.Jewelry.model.state.LockedOutState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho State Pattern của Account
 */
@DisplayName("Account State Pattern Tests")
public class AccountStatePatternTest {

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account(
            1,
            "testuser",
            "hashedPassword123",
            "Test User",
            "ADMIN"
        );
    }

    @Test
    @DisplayName("ACTIVE state - có thể login")
    void testActiveStateCanLogin() {
        account.setState(new ActiveState());
        account.setStatus("ACTIVE");
        
        assertTrue(account.canLogin(), "ACTIVE state phải có thể login");
        assertTrue(account.canAccessSystem(), "ACTIVE state phải có thể truy cập");
        assertTrue(account.canModifyData(), "ACTIVE state phải có thể sửa đổi dữ liệu");
    }

    @Test
    @DisplayName("SUSPENDED state - không thể login")
    void testSuspendedStateCannotLogin() {
        account.setState(new SuspendedState());
        account.setStatus("SUSPENDED");
        
        assertFalse(account.canLogin(), "SUSPENDED state không được login");
        assertFalse(account.canAccessSystem(), "SUSPENDED state không được truy cập");
        assertFalse(account.canModifyData(), "SUSPENDED state không được sửa đổi dữ liệu");
    }

    @Test
    @DisplayName("LOCKED state - không thể login")
    void testLockedStateCannotLogin() {
        account.setState(new LockedOutState());
        account.setStatus("LOCKED");
        
        assertFalse(account.canLogin(), "LOCKED state không được login");
        assertFalse(account.canAccessSystem(), "LOCKED state không được truy cập");
        assertFalse(account.canModifyData(), "LOCKED state không được sửa đổi dữ liệu");
    }

    @Test
    @DisplayName("Chuyển ACTIVE → SUSPENDED")
    void testTransitionActiveToSuspended() {
        account.setState(new ActiveState());
        account.setStatus("ACTIVE");
        
        account.suspend();
        
        assertEquals("SUSPENDED", account.getStatus());
        assertEquals("SUSPENDED", account.getAccountStateName());
        assertFalse(account.canLogin());
    }

    @Test
    @DisplayName("Chuyển SUSPENDED → ACTIVE")
    void testTransitionSuspendedToActive() {
        account.setState(new SuspendedState());
        account.setStatus("SUSPENDED");
        
        account.activate();
        
        assertEquals("ACTIVE", account.getStatus());
        assertEquals("ACTIVE", account.getAccountStateName());
        assertTrue(account.canLogin());
    }

    @Test
    @DisplayName("Chuyển ACTIVE → LOCKED")
    void testTransitionActiveToLocked() {
        account.setState(new ActiveState());
        account.setStatus("ACTIVE");
        
        account.lock();
        
        assertEquals("LOCKED", account.getStatus());
        assertEquals("LOCKED", account.getAccountStateName());
        assertFalse(account.canLogin());
    }

    @Test
    @DisplayName("Chuyển LOCKED → ACTIVE")
    void testTransitionLockedToActive() {
        account.setState(new LockedOutState());
        account.setStatus("LOCKED");
        
        account.activate();
        
        assertEquals("ACTIVE", account.getStatus());
        assertEquals("ACTIVE", account.getAccountStateName());
        assertTrue(account.canLogin());
    }

    @Test
    @DisplayName("State transitions phức tạp")
    void testComplexTransitions() {
        // ACTIVE → SUSPENDED
        account.setState(new ActiveState());
        account.suspend();
        assertEquals("SUSPENDED", account.getAccountStateName());
        
        // SUSPENDED → LOCKED
        account.lock();
        assertEquals("LOCKED", account.getAccountStateName());
        
        // LOCKED → ACTIVE
        account.activate();
        assertEquals("ACTIVE", account.getAccountStateName());
        assertTrue(account.canLogin());
    }

    @Test
    @DisplayName("Lấy thông tin state name")
    void testGetStateName() {
        account.setState(new ActiveState());
        assertEquals("ACTIVE", account.getAccountStateName());
        
        account.setState(new SuspendedState());
        assertEquals("SUSPENDED", account.getAccountStateName());
        
        account.setState(new LockedOutState());
        assertEquals("LOCKED", account.getAccountStateName());
    }
}