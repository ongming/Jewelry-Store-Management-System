package com.example.Jewelry.util;

import com.example.Jewelry.model.entity.Account;
import com.example.Jewelry.model.state.ActiveState;
import com.example.Jewelry.model.state.SuspendedState;
import com.example.Jewelry.model.state.LockedOutState;
import com.example.Jewelry.model.state.InactiveState;

/**
 * Utility class để khởi tạo AccountState dựa trên status từ database
 */
public class AccountStateFactory {
    
    /**
     * Tạo AccountState phù hợp dựa trên status string
     */
    public static void initializeStateFromStatus(Account account) {
        if (account == null) {
            return;
        }
        
        String status = account.getStatus();
        
        if ("SUSPENDED".equalsIgnoreCase(status)) {
            account.setState(new SuspendedState());
        } else if ("LOCKED".equalsIgnoreCase(status)) {
            account.setState(new LockedOutState());
        } else if ("INACTIVE".equalsIgnoreCase(status)) {
            account.setState(new InactiveState());
        } else {
            account.setState(new ActiveState());
        }
    }
    
    /**
     * Tạo AccountState từ tên trạng thái
     */
    public static com.example.Jewelry.model.state.AccountState createState(String stateName) {
        if (stateName == null) {
            return new ActiveState();
        }
        
        return switch (stateName.toUpperCase()) {
            case "SUSPENDED" -> new SuspendedState();
            case "LOCKED" -> new LockedOutState();
            case "INACTIVE" -> new InactiveState();
            default -> new ActiveState();
        };
    }
}
