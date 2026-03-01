package services;

import entities.User;
import entities.Reclamation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import utils.database;

import java.sql.*;

public class ReclamationService {

    private Connection connection;

    public ReclamationService() {
        connection = database.getInstance().getConnection();
    }

    public ObservableList<Reclamation> getAll() {
        ObservableList<Reclamation> reclamations = FXCollections.observableArrayList();
        String query = "SELECT * FROM reclamation";

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {

            while (rs.next()) {
                // 1. Création de l'objet
                Reclamation r = new Reclamation(
                        rs.getString("titre"),
                        rs.getString("categorie"),
                        rs.getString("projet"),
                        rs.getString("statut"),
                        rs.getString("date")
                );

                // 2. CORRECTION ICI : On utilise "idRec"
                r.setId(rs.getInt("idRec"));

                // Si vous avez besoin de récupérer l'ID user pour l'affichage plus tard :
                // r.setUserId(rs.getInt("id_user"));

                reclamations.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération : " + e.getMessage());
        }
        return reclamations;
    }

    public void delete(int id) {
        // CORRECTION ICI : WHERE idRec = ?
        String query = "DELETE FROM reclamation WHERE idRec = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("Réclamation supprimée avec succès !");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression : " + e.getMessage());
        }
    }

    public void add(Reclamation r) {
        // 1. Récupération de l'utilisateur ICI au lieu du Controller
        User userConnecte = UserService.getCurrentUser();

        // Sécurité : Si personne n'est connecté, on arrête ou on met 0
        int userId = (userConnecte != null) ? userConnecte.getId() : 0;

        if (userId == 0) {
            System.err.println("Erreur : Tentative d'ajout sans utilisateur connecté !");
            return;
        }

        String query = "INSERT INTO reclamation (titre, categorie, projet, statut, date, id_user) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, r.getTitre());
            pst.setString(2, r.getCategorie());
            pst.setString(3, r.getProjet());
            pst.setString(4, r.getStatut());
            pst.setString(5, r.getDate());

            // 2. Utilisation de l'ID récupéré localement
            pst.setInt(6, userId);

            pst.executeUpdate();
            System.out.println("Réclamation ajoutée avec succès pour l'user ID: " + userId);
        } catch (SQLException e) {
            System.err.println("Erreur d'ajout : " + e.getMessage());
        }
    }

    public void update(Reclamation r) {
        // CORRECTION ICI : WHERE idRec = ?
        String query = "UPDATE reclamation SET titre = ?, categorie = ?, projet = ?, statut = ? WHERE idRec = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, r.getTitre());
            pst.setString(2, r.getCategorie());
            pst.setString(3, r.getProjet());
            pst.setString(4, r.getStatut());

            // On utilise l'ID de l'objet pour trouver la ligne (idRec)
            pst.setInt(5, r.getId());

            pst.executeUpdate();
            System.out.println("Réclamation modifiée avec succès !");
        } catch (SQLException e) {
            System.err.println("Erreur de modification : " + e.getMessage());
        }
    }
    public ObservableList<Reclamation> getReclamationsByUserId(int userId) {
        ObservableList<Reclamation> list = FXCollections.observableArrayList();
        // Assure-toi que le nom de la colonne étrangère est correct (ex: id_user ou user_id)
        String query = "SELECT * FROM reclamation WHERE id_user = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Reclamation r = new Reclamation(
                        rs.getString("titre"),
                        rs.getString("categorie"),
                        rs.getString("projet"),
                        rs.getString("statut"),
                        rs.getString("date")
                );
                r.setId(rs.getInt("idRec")); // Très important pour la modif/suppression
                // r.setUserId(rs.getInt("id_user")); // Optionnel
                list.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erreur recuperation user rec: " + e.getMessage());
        }
        return list;
    }
}