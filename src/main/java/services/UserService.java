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

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public boolean authenticate(String email, String password) throws SQLException {
        String req = "SELECT id, nom, prenom, email, telephone, role, departement, statut, date_inscription, imagelink, face_id FROM utilisateurs WHERE email = ? AND password = ?";
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
                    rs.getBytes("imagelink"),
                    rs.getString("face_id")  // Ajout du face_id
            );
            return true;
        }
        return false;
    }

    public void signup(User user, String password) throws SQLException {
        String req = "INSERT INTO utilisateurs (nom, prenom, email, telephone, role, departement, statut, date_inscription, password, imagelink, face_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
        ps.setBytes(10, user.getImageData());
        ps.setString(11, user.getFaceId()); // Ajout du face_id

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            user.setId(rs.getInt(1));
        }
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
        String req = "SELECT id, nom, prenom, email, telephone, role, departement, statut, date_inscription, imagelink, face_id FROM utilisateurs";
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
                    rs.getBytes("imagelink"),
                    rs.getString("face_id") // Ajout du face_id
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

    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM utilisateurs WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public void modifierRole(int id, String newRole) throws SQLException {
        String req = "UPDATE utilisateurs SET role = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, newRole);
        ps.setInt(2, id);
        ps.executeUpdate();
    }

    public void modifierProfil(User user) throws SQLException {
        // On ne modifie pas le face_id ici
        String req = "UPDATE utilisateurs SET nom=?, prenom=?, email=?, telephone=?, departement=?, imagelink=? WHERE id=?";

        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, user.getName());
        ps.setString(2, user.getFirstName());
        ps.setString(3, user.getEmail());
        ps.setString(4, user.getPhone());
        ps.setString(5, user.getDepartment());
        ps.setBytes(6, user.getImageData());
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

        currentUser = null;
        System.out.println("Compte supprimé et session fermée.");
    }

    // Nouvelle méthode : récupérer un utilisateur par email (utilisée après reconnaissance faciale)
    public User getUserByEmail(String email) throws SQLException {
        String req = "SELECT id, nom, prenom, email, telephone, role, departement, statut, date_inscription, imagelink, face_id FROM utilisateurs WHERE email = ?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new User(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("email"),
                    rs.getString("telephone"),
                    rs.getString("role"),
                    rs.getString("departement"),
                    rs.getString("statut"),
                    rs.getString("date_inscription"),
                    rs.getBytes("imagelink"),
                    rs.getString("face_id")
            );
        }
        return null;
    }

    // Nouvelle méthode : mettre à jour le face_id d'un utilisateur (après inscription ou réinscription)
    public void updateFaceId(int userId, String faceId) throws SQLException {
        String req = "UPDATE utilisateurs SET face_id = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, faceId);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }
    public void updatePassword(int userId, String newPassword) throws SQLException {
        String req = "UPDATE utilisateurs SET password = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, newPassword);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }
}