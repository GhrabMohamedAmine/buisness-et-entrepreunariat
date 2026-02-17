package com.example.testp1.services;

import com.example.testp1.entities.Transaction;
import com.example.testp1.model.TransactionDAO;

import java.sql.SQLException;
import java.util.List;

public class ServiceTransaction implements IService<Transaction> {

    private final TransactionDAO transactionDAO;

    public ServiceTransaction() {
        this.transactionDAO = new TransactionDAO();
    }

    @Override
    public void add(Transaction t) throws SQLException {
        if (t.getCost() > 0) {
            transactionDAO.add(t);
        } else {
            throw new SQLException("Transaction cost must be positive.");
        }
    }

    @Override
    public void update(Transaction t) throws SQLException {
        if (t.getId() > 0) {
            transactionDAO.update(t);
        }
    }

    @Override
    public void delete(Transaction t) throws SQLException {
        transactionDAO.delete(t.getId());
    }

    @Override
    public List<Transaction> getAll() throws SQLException {
        return transactionDAO.getAll();
    }
    public List<Transaction> getTransactionsByBudget(int budgetId) throws SQLException {
        return transactionDAO.getByBudgetId(budgetId);
    }
}
