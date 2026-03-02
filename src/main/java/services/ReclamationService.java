package services;

import entities.User;
import entities.Reclamation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import utils.database;

import java.sql.*;
import java.io.ByteArrayInputStream;

public class ReclamationService {

    private Connection connection;

    public ReclamationService() {
        connection = database.getInstance().getConnection();
    }

    /**
     * Récupère toutes les réclamations (sans le fichier pour des raisons de performance).
     */
    public ObservableList<Reclamation> getAll() {
        ObservableList<Reclamation> reclamations = FXCollections.observableArrayList();
        String query = "SELECT idRec, titre, categorie, projet, statut, date, id_user FROM reclamation";

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {

            while (rs.next()) {
                Reclamation r = new Reclamation(
                        rs.getString("titre"),
                        rs.getString("categorie"),
                        rs.getString("projet"),
                        rs.getString("statut"),
                        rs.getString("date")
                );
                r.setId(rs.getInt("idRec"));
                r.setUserId(rs.getInt("id_user")); // si vous avez ce champ
                reclamations.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération : " + e.getMessage());
        }
        return reclamations;
    }

    /**
     * Supprime une réclamation par son ID.
     */
    public void delete(int id) {
        String query = "DELETE FROM reclamation WHERE idRec = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("Réclamation supprimée avec succès !");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression : " + e.getMessage());
        }
    }

    /**
     * Ajoute une nouvelle réclamation (y compris le fichier s'il est présent).
     */
    public void add(Reclamation r) {
        User userConnecte = UserService.getCurrentUser();
        int userId = (userConnecte != null) ? userConnecte.getId() : 0;

        if (userId == 0) {
            System.err.println("Erreur : Tentative d'ajout sans utilisateur connecté !");
            return;
        }

        String query = "INSERT INTO reclamation (titre, categorie, projet, statut, date, id_user, fichier) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, r.getTitre());
            pst.setString(2, r.getCategorie());
            pst.setString(3, r.getProjet());
            pst.setString(4, r.getStatut());
            pst.setString(5, r.getDate());
            pst.setInt(6, userId);

            byte[] fichier = r.getFichier();
            if (fichier != null) {
                pst.setBinaryStream(7, new ByteArrayInputStream(fichier), fichier.length);
            } else {
                pst.setNull(7, Types.BLOB);
            }

            pst.executeUpdate();
            System.out.println("Réclamation ajoutée avec succès pour l'user ID: " + userId);
        } catch (SQLException e) {
            System.err.println("Erreur d'ajout : " + e.getMessage());
        }
    }

    /**
     * Met à jour une réclamation existante (y compris le fichier).
     * Le fichier sera remplacé par celui présent dans l'objet (peut être null).
     */
    public void update(Reclamation r) {
        String query = "UPDATE reclamation SET titre = ?, categorie = ?, projet = ?, statut = ?, fichier = ? WHERE idRec = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, r.getTitre());
            pst.setString(2, r.getCategorie());
            pst.setString(3, r.getProjet());
            pst.setString(4, r.getStatut());

            byte[] fichier = r.getFichier();
            if (fichier != null) {
                pst.setBinaryStream(5, new ByteArrayInputStream(fichier), fichier.length);
            } else {
                pst.setNull(5, Types.BLOB);
            }

            pst.setInt(6, r.getId());

            pst.executeUpdate();
            System.out.println("Réclamation modifiée avec succès !");
        } catch (SQLException e) {
            System.err.println("Erreur de modification : " + e.getMessage());
        }
    }

    /**
     * Récupère une réclamation par son ID (y compris le fichier).
     * Utilisé pour l'édition afin de conserver le fichier existant.
     */
    public Reclamation getById(int id) {
        String query = "SELECT * FROM reclamation WHERE idRec = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                Reclamation r = new Reclamation(
                        rs.getString("titre"),
                        rs.getString("categorie"),
                        rs.getString("projet"),
                        rs.getString("statut"),
                        rs.getString("date")
                );
                r.setId(rs.getInt("idRec"));
                r.setUserId(rs.getInt("id_user"));
                r.setFichier(rs.getBytes("fichier")); // lecture du BLOB
                return r;
            }
        } catch (SQLException e) {
            System.err.println("Erreur getById : " + e.getMessage());
        }
        return null;
    }

    /**
     * Récupère toutes les réclamations d'un utilisateur donné (sans le fichier).
     */
    public ObservableList<Reclamation> getReclamationsByUserId(int userId) {
        ObservableList<Reclamation> list = FXCollections.observableArrayList();
        String query = "SELECT idRec, titre, categorie, projet, statut, date FROM reclamation WHERE id_user = ?";

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
                r.setId(rs.getInt("idRec"));
                list.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erreur recuperation user rec: " + e.getMessage());
        }
        return list;
    }
}