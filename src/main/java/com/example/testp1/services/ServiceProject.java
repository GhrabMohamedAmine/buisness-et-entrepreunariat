package com.example.testp1.services;

import com.example.testp1.model.ProjectDAO;

import java.util.List;

public class ServiceProject {
    private final ProjectDAO projectDAO;

    public ServiceProject() {
        this.projectDAO = new ProjectDAO();
    }

    public List<String> getAvailableProjectNames() {
        return projectDAO.getAvailableProjectNames();
    }
}
