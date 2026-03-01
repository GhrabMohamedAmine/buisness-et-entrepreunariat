package entities;

public class Quiz {

    private int id;
    private String question;
    private String r1;
    private String r2;
    private String r3;
    private String image;
    private int formationId;
    private int correct; // ⭐ NOUVEAU

    public Quiz(){}

    public Quiz(int id, String question, String r1, String r2, String r3,
                String image, int formationId, int correct) {
        this.id = id;
        this.question = question;
        this.r1 = r1;
        this.r2 = r2;
        this.r3 = r3;
        this.image = image;
        this.formationId = formationId;
        this.correct = correct;
    }

    public Quiz(String question, String r1, String r2, String r3,
                String image, int formationId, int correct) {

        this.question = question;
        this.r1 = r1;
        this.r2 = r2;
        this.r3 = r3;
        this.image = image;
        this.formationId = formationId;
        this.correct = correct;
    }


    public int getId(){ return id; }
    public String getQuestion(){ return question; }
    public String getR1(){ return r1; }
    public String getR2(){ return r2; }
    public String getR3(){ return r3; }
    public String getImage(){ return image; }
    public int getFormationId(){ return formationId; }
    public int getCorrect(){ return correct; }
}
