package esprit.tn.pi_kavafx.entities;

import java.sql.Timestamp;

public class Resultat {

    private int id;
    private int formationId;
    private int score;
    private int total;
    private Timestamp datePassage;

    public Resultat(int formationId, int score, int total) {
        this.formationId = formationId;
        this.score = score;
        this.total = total;
    }

    public Resultat(int id, int formationId, int score, int total, Timestamp datePassage) {
        this.id = id;
        this.formationId = formationId;
        this.score = score;
        this.total = total;
        this.datePassage = datePassage;
    }

    public int getId() { return id; }
    public int getFormationId() { return formationId; }
    public int getScore() { return score; }
    public int getTotal() { return total; }
    public Timestamp getDatePassage() { return datePassage; }
}
