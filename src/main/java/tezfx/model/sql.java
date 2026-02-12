package tezfx.model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static java.awt.AWTEventMulticaster.add;

public class sql {
    public List<Project> getAllProjects() {
        List<Project> projects = new ArrayList<>();
        String query = "SELECT * FROM projects";

        try (Connection conn =  databaseconnection.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query)) {
            while(rs.next()){
                projects.add(new Project(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getInt("progress"),
                        rs.getDouble("budget"),
                        rs.getDate("start_date"),
                        rs.getDate("end_date"),
                        rs.getInt("assigned_to"),
                        rs.getInt("created_by")
                ));

            }


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return projects;
    }
    public void addProject(Project project) {
        String query = "INSERT INTO projects (name, description, progress, budget, start_date, end_date, assigned_to, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseconnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, project.getName());
            pstmt.setString(2, project.getDescription());
            pstmt.setInt(3, project.getProgress());
            pstmt.setDouble(4, project.getBudget());
            String startDateStr = project.getStartDate();
            if (startDateStr != null && startDateStr.length() == 10) {
                pstmt.setDate(5, java.sql.Date.valueOf(startDateStr));
            } else {
                pstmt.setNull(5, java.sql.Types.DATE);
            }
           String endDateStr = project.getEndDate();
            if (endDateStr != null && endDateStr.length() == 10) {
                pstmt.setDate(6, java.sql.Date.valueOf(endDateStr));
            } else {
                pstmt.setNull(6, java.sql.Types.DATE);
            }
            pstmt.setInt(7, project.getAssignedTo());
            pstmt.setInt(8, project.getCreatedby());
            pstmt.executeUpdate();
            System.out.println("Project added successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
}
}

