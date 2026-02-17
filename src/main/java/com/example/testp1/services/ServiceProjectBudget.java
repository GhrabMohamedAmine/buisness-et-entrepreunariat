package com.example.testp1.services;

import com.example.testp1.entities.ProjectBudget;
import com.example.testp1.model.ProjectBudgetDAO;
import java.sql.SQLException;
import java.util.List;

public class ServiceProjectBudget implements IService<ProjectBudget> {

    private final ProjectBudgetDAO budgetDAO;

    public ServiceProjectBudget() {
        this.budgetDAO = new ProjectBudgetDAO();
    }

    @Override
    public void add(ProjectBudget budget) throws SQLException {
        if (budget.getTotalBudget() >= 0 && budget.getName() != null) {
            budgetDAO.add(budget);
        } else {
            throw new SQLException("Validation failed: Budget must be non-negative and have a name.");
        }
    }

    @Override
    public void update(ProjectBudget budget) throws SQLException {
        if (budget.getId() > 0) {
            budgetDAO.update(budget);
        } else {
            throw new SQLException("Cannot update: Invalid Budget ID.");
        }
    }

    @Override
    public void delete(ProjectBudget budget) throws SQLException {
        if (budget.getId() > 0) {
            budgetDAO.delete(budget.getId());
        } else {
            throw new SQLException("Cannot delete: Invalid Budget ID.");
        }
    }

    @Override
    public List<ProjectBudget> getAll() throws SQLException {
        return budgetDAO.getAll();
    }

    public ProjectBudget getById(int id) throws SQLException {
        return budgetDAO.getById(id);
    }
}