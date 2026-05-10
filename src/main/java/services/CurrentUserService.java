package services;

import entities.User;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CurrentUserService {
    private static User currentUser;

    public User getCurrentUser() {
        if (currentUser == null) {
            currentUser = loadManagerUser();
        }
        return currentUser;
    }

    public int getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public boolean isCurrentUserManager() {
        return getCurrentUser().isManager();
    }

    private User loadManagerUser() {
        String managerQuery = "SELECT id, nom, prenom, role FROM utilisateurs " +
                "WHERE UPPER(COALESCE(role, '')) LIKE '%MANAGER%' " +
                "ORDER BY id LIMIT 1";
        try (Connection conn = MyDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(managerQuery);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return mapUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String fallbackQuery = "SELECT id, nom, prenom, role FROM utilisateurs ORDER BY id LIMIT 1";
        try (Connection conn = MyDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(fallbackQuery);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return mapUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new User(1, "Manager", "User", "MANAGER");
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getString("role")
        );
    }
}
