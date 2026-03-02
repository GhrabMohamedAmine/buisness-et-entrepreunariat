import entities.Formation;
import entities.Resultat;
import org.junit.jupiter.api.*;
import services.FormationService;
import services.ResultatService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ResultatServiceTest {

    static ResultatService resultatService;
    static FormationService formationService;

    static int formationIdTest;
    static int scoreTest = 3;
    static int totalTest = 5;

    // ================= INIT =================
    @BeforeAll
    static void setup(){
        resultatService = new ResultatService();
        formationService = new FormationService();
        System.out.println("=== TEST RESULTAT SERVICE ===");
    }

    // ================= CREATE SUPPORT FORMATION =================
    @Test
    @Order(1)
    void createFormationSupport(){

        String uniqueTitle = "Formation Resultat TEST_" + System.currentTimeMillis();

        Formation f = new Formation(
                uniqueTitle,
                "formation support pour test resultat",
                "v1.mp4",
                "v2.mp4",
                "v3.mp4"
        );

        formationService.add(f);

        // retrieve inserted formation
        List<Formation> list = formationService.getAll();

        Formation found = null;
        for(Formation formation : list){
            if(formation.getTitre().equals(uniqueTitle)){
                found = formation;
                break;
            }
        }

        assertNotNull(found, "Formation support non trouvée !");
        formationIdTest = found.getId();
    }

    // ================= SAVE RESULT =================
    @Test
    @Order(2)
    void testSaveResultat(){

        Resultat r = new Resultat(
                0,
                formationIdTest,
                scoreTest,
                totalTest,
                null
        );

        resultatService.save(r);

        List<Resultat> list = resultatService.getAll();

        boolean found = false;

        for(Resultat res : list){
            if(res.getFormationId() == formationIdTest
                    && res.getScore() == scoreTest
                    && res.getTotal() == totalTest){
                found = true;
                break;
            }
        }

        assertTrue(found, "Le résultat sauvegardé n'existe pas !");
    }

    // ================= VERIFY RETRIEVAL =================
    @Test
    @Order(3)
    void testGetAll(){

        List<Resultat> list = resultatService.getAll();

        assertFalse(list.isEmpty(), "La liste des résultats ne doit pas être vide");
    }

    // ================= CLEAN DATABASE =================
    @AfterAll
    static void cleanup(){

        try {
            // delete formation (cascade will delete results if FK configured)
            formationService.delete(formationIdTest);

        } catch (Exception ignored){}
    }
}
