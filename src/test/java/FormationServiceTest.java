
import entities.Formation;
import org.junit.jupiter.api.*;
import services.FormationService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FormationServiceTest {

    static FormationService service;
    static int idFormationTest;
    static String titreTest;

    // ================= INITIALISATION =================
    @BeforeAll
    static void setup(){
        service = new FormationService();
        System.out.println("=== TEST FORMATION SERVICE ===");
    }

    // ================= TEST 1 : ADD =================
    @Test
    @Order(1)
    void testAddFormation(){

        // UNIQUE TITLE (important to avoid DB conflicts)
        titreTest = "JUnit Formation TEST_" + System.currentTimeMillis();

        Formation f = new Formation(
                titreTest,
                "Description test unitaire",
                "v1.mp4",
                "v2.mp4",
                "v3.mp4"
        );

        service.add(f);

        List<Formation> list = service.getAll();
        assertFalse(list.isEmpty());

        // find OUR inserted formation
        Formation found = null;

        for(Formation formation : list){
            if(formation.getTitre().equals(titreTest)){
                found = formation;
                break;
            }
        }

        assertNotNull(found, "La formation insérée n'a pas été trouvée !");
        idFormationTest = found.getId();
    }

    // ================= TEST 2 : GET BY ID =================
    @Test
    @Order(2)
    void testGetById(){

        Formation f = service.getById(idFormationTest);

        assertNotNull(f, "getById a retourné null !");
        assertEquals(titreTest, f.getTitre());
    }

    // ================= TEST 3 : UPDATE =================
    @Test
    @Order(3)
    void testUpdateFormation(){

        Formation f = service.getById(idFormationTest);
        assertNotNull(f);

        String newTitle = "Formation Modifiée TEST";
        f.setTitre(newTitle);

        service.update(f);

        Formation updated = service.getById(idFormationTest);

        assertNotNull(updated);
        assertEquals(newTitle, updated.getTitre());
    }

    // ================= TEST 4 : DELETE =================
    @Test
    @Order(4)
    void testDeleteFormation(){

        service.delete(idFormationTest);

        Formation f = service.getById(idFormationTest);

        assertNull(f, "La formation devrait être supprimée !");
    }

    // ================= CLEAN DATABASE =================
    @AfterAll
    static void cleanDatabase(){

        try {
            List<Formation> list = service.getAll();

            for(Formation f : list){
                if(f.getTitre().startsWith("JUnit Formation TEST_")
                        || f.getTitre().equals("Formation Modifiée TEST")){
                    service.delete(f.getId());
                }
            }
        } catch (Exception ignored){}
    }
}
