
import entities.*;
import org.junit.jupiter.api.*;
import services.FormationService;
import services.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QuizServiceTest {

    static QuizService quizService;
    static FormationService formationService;

    static int formationIdTest;
    static int quizIdTest;
    static String uniqueQuestion;

    // ================= INIT =================
    @BeforeAll
    static void setup(){
        quizService = new QuizService();
        formationService = new FormationService();
        System.out.println("=== TEST QUIZ SERVICE ===");
    }

    // ================= CREATE SUPPORT FORMATION =================
    @Test
    @Order(1)
    void createFormationForQuiz(){

        String uniqueTitle = "Formation Quiz TEST_" + System.currentTimeMillis();

        Formation f = new Formation(
                uniqueTitle,
                "formation support pour test quiz",
                "v1.mp4",
                "v2.mp4",
                "v3.mp4"
        );

        formationService.add(f);

        // find inserted formation
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

    // ================= ADD QUIZ =================
    @Test
    @Order(2)
    void testAddQuiz(){

        uniqueQuestion = "2+2=? TEST_" + System.currentTimeMillis();

        Quiz q = new Quiz(
                uniqueQuestion,
                "3",
                "4",
                "5",
                null,
                formationIdTest,
                2
        );

        quizService.add(q);

        List<Quiz> list = quizService.getByFormationId(formationIdTest);

        Quiz found = null;
        for(Quiz quiz : list){
            if(quiz.getQuestion().equals(uniqueQuestion)){
                found = quiz;
                break;
            }
        }

        assertNotNull(found, "Quiz inséré non trouvé !");
        quizIdTest = found.getId();
    }

    // ================= GET QUIZ =================
    @Test
    @Order(3)
    void testGetQuiz(){

        List<Quiz> list = quizService.getByFormationId(formationIdTest);

        boolean exists = false;

        for(Quiz q : list){
            if(q.getId() == quizIdTest){
                exists = true;
                assertEquals(uniqueQuestion, q.getQuestion());
            }
        }

        assertTrue(exists, "Le quiz ajouté n'existe pas !");
    }

    // ================= DELETE QUIZ =================
    @Test
    @Order(4)
    void testDeleteQuiz(){

        quizService.delete(quizIdTest);

        List<Quiz> list = quizService.getByFormationId(formationIdTest);

        for(Quiz q : list){
            assertNotEquals(quizIdTest, q.getId());
        }
    }

    // ================= CLEAN DATABASE =================
    @AfterAll
    static void cleanup(){

        try {
            // delete all test quizzes
            List<Quiz> quizzes = quizService.getByFormationId(formationIdTest);
            for(Quiz q : quizzes){
                quizService.delete(q.getId());
            }

            // delete formation support
            formationService.delete(formationIdTest);

        } catch (Exception ignored){}
    }
}
