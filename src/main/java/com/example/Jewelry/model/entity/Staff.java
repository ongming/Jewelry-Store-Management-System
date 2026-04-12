package com.example.Jewelry.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "staff")
@PrimaryKeyJoinColumn(name = "account_id")
public class Staff extends Account {

    @Column(name = "staff_id", nullable = false, unique = true)
    private int staffId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_admin_account_id")
    private Admin managerAdmin;

    @OneToMany(mappedBy = "staff")
    private List<Order> createdOrders = new ArrayList<>();

    @OneToMany(mappedBy = "staff")
    private List<ImportReceipt> managedImportReceipts = new ArrayList<>();

    public Staff() {
    }

    public Staff(int accountId, String username, String passwordHash, String fullName, String roleName, int staffId) {
        super(accountId, username, passwordHash, fullName, roleName, "ACTIVE");
        this.staffId = staffId;
    }

    public int getStaffId() {
        return staffId;
    }

    public void setStaffId(int staffId) {
        this.staffId = staffId;
    }

    public Admin getManagerAdmin() {
        return managerAdmin;
    }

    public void setManagerAdmin(Admin managerAdmin) {
        this.managerAdmin = managerAdmin;
    }

    public List<Order> getCreatedOrders() {
        return createdOrders;
    }

    public void setCreatedOrders(List<Order> createdOrders) {
        this.createdOrders = createdOrders;
    }

    public List<ImportReceipt> getManagedImportReceipts() {
        return managedImportReceipts;
    }

    public void setManagedImportReceipts(List<ImportReceipt> managedImportReceipts) {
        this.managedImportReceipts = managedImportReceipts;
    }
}