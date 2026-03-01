package entities;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Reclamation {
    private int id;
    private int userId; // <--- NOUVEAU CHAMP : Clé étrangère

    private final StringProperty titre = new SimpleStringProperty();
    private final StringProperty categorie = new SimpleStringProperty();
    private final StringProperty projet = new SimpleStringProperty();
    private final StringProperty statut = new SimpleStringProperty();
    private final StringProperty date = new SimpleStringProperty();

    // Constructeur
    public Reclamation(String titre, String categorie, String projet, String statut, String date) {
        this.titre.set(titre);
        this.categorie.set(categorie);
        this.projet.set(projet);
        this.statut.set(statut);
        this.date.set(date);
    }

    // --- Getters et Setters pour userId ---
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    // Reste des getters/setters...
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitre() { return titre.get(); }
    public String getCategorie() { return categorie.get(); }
    public String getProjet() { return projet.get(); }
    public String getStatut() { return statut.get(); }
    public String getDate() { return date.get(); }
}