package com.example.Jewelry.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "admin")
@PrimaryKeyJoinColumn(name = "account_id")
public class Admin extends Staff {

    @OneToMany(mappedBy = "managerAdmin")
    private List<Staff> managedStaffs = new ArrayList<>();

    public Admin() {
    }

    public List<Staff> getManagedStaffs() {
        return managedStaffs;
    }

    public void setManagedStaffs(List<Staff> managedStaffs) {
        this.managedStaffs = managedStaffs;
    }
}
