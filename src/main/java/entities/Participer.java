package entities;

import java.sql.Timestamp;

public class Participer {

    private int id;
    private int userId;
    private int formationId;

    private Timestamp dateInscription;
    private int progression;
    private String statut; // EN_COURS, TERMINE, REUSSI, ECHOUE

    public Participer(){}

    public Participer(int userId, int formationId){
        this.userId = userId;
        this.formationId = formationId;
        this.progression = 0;
        this.statut = "EN_COURS";
    }

    public Participer(int id,int userId,int formationId,
                      Timestamp dateInscription,int progression,String statut){
        this.id=id;
        this.userId=userId;
        this.formationId=formationId;
        this.dateInscription=dateInscription;
        this.progression=progression;
        this.statut=statut;
    }

    public int getId(){return id;}
    public int getUserId(){return userId;}
    public int getFormationId(){return formationId;}
    public Timestamp getDateInscription(){return dateInscription;}
    public int getProgression(){return progression;}
    public String getStatut(){return statut;}

    public void setProgression(int progression){this.progression=progression;}
    public void setStatut(String statut){this.statut=statut;}
}
