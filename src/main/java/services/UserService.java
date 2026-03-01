package services;

import entities.User;
import utils.database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private Connection connection;
    private static User currentUser;

    public UserService() {
        connection = database.getInstance().getConnection();
    }

    public boolean authenticate(String email, String password) throws SQLException {
        String req = "SELECT * FROM utilisateurs WHERE email = ? AND password = ?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, email);
        ps.setString(2, password);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            currentUser = new User(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("email"),
                    rs.getString("telephone"),
                    rs.getString("role"),
                    rs.getString("departement"),
                    rs.getString("statut"),
                    rs.getString("date_inscription"),
                    rs.getString("imagelink") // Ajout ici
            );
            return true;
        }
        return false;
    }


    public void signupAndLogin(User user, String password) throws SQLException {
        // Ajout de imagelink dans la requête INSERT
        String req = "INSERT INTO utilisateurs (nom, prenom, email, telephone, role, departement, statut, date_inscription, password, imagelink) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, user.getName());
        ps.setString(2, user.getFirstName());
        ps.setString(3, user.getEmail());
        ps.setString(4, user.getPhone());
        ps.setString(5, user.getRole());
        ps.setString(6, user.getDepartment());
        ps.setString(7, user.getStatus());
        ps.setString(8, user.getJoinedDate());
        ps.setString(9, password);
        ps.setString(10, user.getImageLink()); // Ajout ici

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            user.setId(rs.getInt(1));
        }
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void logout() {
        currentUser = null;
        System.out.println("Session clôturée.");
    }
    public List<User> recupererTous() throws SQLException {
        List<User> utilisateurs = new ArrayList<>();
        String req = "SELECT * FROM utilisateurs";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            utilisateurs.add(new User(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("email"),
                    rs.getString("telephone"),
                    rs.getString("role"),
                    rs.getString("departement"),
                    rs.getString("statut"),
                    rs.getString("date_inscription"),
                    rs.getString("imagelink") // Ajout ici
            ));
        }
        return utilisateurs;
    }
    public void modifierStatut(int id, String newStatus) throws SQLException {
        String req = "UPDATE utilisateurs SET statut = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, newStatus);
        ps.setInt(2, id);
        ps.executeUpdate();
    }
    // Add this inside UserService.java
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM utilisateurs WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // Add inside UserService.java
    public void modifierRole(int id, String newRole) throws SQLException {
        String req = "UPDATE utilisateurs SET role = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, newRole);
        ps.setInt(2, id);
        ps.executeUpdate();
    }
    // Dans UserService.java

    public void modifierProfil(User user) throws SQLException {
        // J'ai ajouté "imagelink=?" dans la requête
        String req = "UPDATE utilisateurs SET nom=?, prenom=?, email=?, telephone=?, departement=?, imagelink=? WHERE id=?";

        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, user.getName());
        ps.setString(2, user.getFirstName());
        ps.setString(3, user.getEmail());
        ps.setString(4, user.getPhone());
        ps.setString(5, user.getDepartment());

        // Sauvegarde du chemin de l'image
        ps.setString(6, user.getImageLink());

        ps.setInt(7, user.getId());

        ps.executeUpdate();

        // Mettre à jour l'utilisateur en mémoire cache
        currentUser = user;
        System.out.println("Profil et image mis à jour avec succès !");
    }

    public void supprimerCompte(int id) throws SQLException {
        String req = "DELETE FROM utilisateurs WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();

        // IMPORTANT : On vide la session
        currentUser = null;
        System.out.println("Compte supprimé et session fermée.");
    }

}