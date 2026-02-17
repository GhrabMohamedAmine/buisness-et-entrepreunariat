package com.example.testp1.model;

import com.example.testp1.model.DB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectDAO {

    private final Connection connection;

    public ProjectDAO() {
        this.connection = DB.getInstance().getConx();
    }

    public List<String> getAvailableProjectNames() {
        List<String> projectNames = new ArrayList<>();

        // SQL logic: Find projects that have no match in the project_budget table
        String req = "SELECT * FROM projects";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                projectNames.add(rs.getString("name"));
            }

        } catch (SQLException e) {
            System.err.println("SQL Error (Fetching available projects): " + e.getMessage());
        }

        return projectNames;
    }
    public int getIdByName(String projectName) {
        int projectId = 0;
        String req = "SELECT id FROM projects WHERE name = ?";
        try (PreparedStatement pst = connection.prepareStatement(req)) {
            pst.setString(1, projectName);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    projectId = rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Error (Fetching ID by Name): " + e.getMessage());
        }
        return projectId;
    }

    public String getNameById(int id) {
        String projectName = "Unknown Project";
        String query = "SELECT name FROM projects WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    projectName = rs.getString("name");
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Error (Fetching Name by ID): " + e.getMessage());
        }
        return projectName;
    }

    public java.time.LocalDate getEndDateByName(String projectName) {
        String query = "SELECT end_date FROM projects WHERE name = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, projectName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Returns the date as a LocalDate
                    java.sql.Date sqlDate = rs.getDate("end_date");
                    return (sqlDate != null) ? sqlDate.toLocalDate() : null;
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Error (getEndDateByName): " + e.getMessage());
        }
        return null;
    }
}