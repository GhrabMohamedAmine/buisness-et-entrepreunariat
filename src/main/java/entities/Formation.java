package entities;

public class Formation {

    private int id;
    private String titre;
    private String description;
    private String video1;
    private String video2;
    private String video3;

    public Formation() {}

    public Formation(String titre, String description, String video1, String video2, String video3) {
        this.titre = titre;
        this.description = description;
        this.video1 = video1;
        this.video2 = video2;
        this.video3 = video3;
    }

    public Formation(int id, String titre, String description, String video1, String video2, String video3) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.video1 = video1;
        this.video2 = video2;
        this.video3 = video3;
    }

    public int getId() { return id; }
    public String getTitre() { return titre; }
    public String getDescription() { return description; }
    public String getVideo1() { return video1; }
    public String getVideo2() { return video2; }
    public String getVideo3() { return video3; }

    public void setId(int id) { this.id = id; }
    public void setTitre(String titre) { this.titre = titre; }
    public void setDescription(String description) { this.description = description; }
    public void setVideo1(String video1) { this.video1 = video1; }
    public void setVideo2(String video2) { this.video2 = video2; }
    public void setVideo3(String video3) { this.video3 = video3; }
}
