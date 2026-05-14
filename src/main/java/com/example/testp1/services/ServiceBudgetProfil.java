package com.example.testp1.services;

import com.example.testp1.entities.BudgetProfil;
import com.example.testp1.model.BudgetProfilDAO;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class ServiceBudgetProfil implements IService<BudgetProfil> {

    private final BudgetProfilDAO profilDAO;

    public ServiceBudgetProfil() {
        this.profilDAO = new BudgetProfilDAO();
    }

    @Override
    public void add(BudgetProfil p) throws SQLException {
        // Business Rule: Check if a profile already exists before adding as ACTIVE
        if ("ACTIVE".equalsIgnoreCase(p.getStatus())) {
            BudgetProfil existing = profilDAO.getActiveProfile();
            if (existing != null) {
                throw new SQLException("An ACTIVE Budget Profile already exists.");
            }
        }
        profilDAO.add(p);
    }

    @Override
    public void update(BudgetProfil p) throws SQLException {
        if (p.getId() > 0) {
            if ("ACTIVE".equalsIgnoreCase(p.getStatus())) {
                BudgetProfil existing = profilDAO.getActiveProfile();
                if (existing != null && existing.getId() != p.getId()) {
                    throw new SQLException("Another ACTIVE Budget Profile already exists.");
                }
            }
            profilDAO.update(p);
        } else {
            throw new SQLException("Cannot update: Invalid Profile ID.");
        }
    }

    @Override
    public void delete(BudgetProfil p) throws SQLException {
        if (p.getId() > 0) {
            profilDAO.delete(p.getId());
        } else {
            throw new SQLException("Cannot delete: Invalid Profile ID.");
        }
    }

    @Override
    public List<BudgetProfil> getAll() throws SQLException {
        return profilDAO.getAllProfiles();
    }

    public void syncdata() throws SQLException {
        profilDAO.syncProfileTotalExpense();
    }

    public BudgetProfil getActiveProfile() throws SQLException {
        return profilDAO.getActiveProfile();
    }

    public BudgetProfil getById(int id) throws SQLException {
        return profilDAO.getById(id);
    }
}