import entities.Reclamation;
import entities.User;
import org.junit.jupiter.api.*;
import services.ReclamationService;
import services.UserService;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReclamationServiceTest {

    private static ReclamationService reclamationService;
    private static UserService userService;

    private static int currentUserId;
    private static int createdReclamationId;

    private static final String TEST_EMAIL = "test_rec_" + System.currentTimeMillis() + "@gmail.com";

    @BeforeAll
    public static void setup() throws SQLException {
        reclamationService = new ReclamationService();
        userService = new UserService();

        System.out.println("--- PRÉPARATION : Création et Connexion User ---");

        // Utilisation du constructeur complet (11 paramètres) avec null pour imageData et faceId
        User user = new User(
                0,                           // id (sera généré)
                "Tester",                     // nom
                "Reclam",                     // prénom
                TEST_EMAIL,                   // email
                "00000000",                    // téléphone
                "Client",                      // rôle
                "IT",                          // département
                "Actif",                       // statut
                "2024-01-01",                  // date d'inscription
                null,                          // imageData (byte[])
                null                           // faceId
        );

        userService.signup(user, "password123");

        boolean loginSuccess = userService.authenticate(TEST_EMAIL, "password123");
        assertTrue(loginSuccess, "L'authentification doit réussir après le signup !");

        User connectedUser = UserService.getCurrentUser();
        assertNotNull(connectedUser, "Le user doit être connecté pour tester les réclamations !");

        currentUserId = connectedUser.getId();
        System.out.println("✅ User enregistré et connecté avec succès. ID : " + currentUserId);
    }

    @Test
    @Order(1)
    @DisplayName("1. Test méthode add()")
    public void testAdd() {
        Reclamation r = new Reclamation(
                "PC en panne",
                "Matériel",
                "Migration Windows",
                "En cours",
                "2024-05-20"
        );

        reclamationService.add(r);

        List<Reclamation> list = reclamationService.getReclamationsByUserId(currentUserId);
        Optional<Reclamation> recFound = list.stream()
                .filter(rec -> rec.getTitre().equals("PC en panne"))
                .findFirst();

        assertTrue(recFound.isPresent(), "La réclamation devrait être trouvée");
        createdReclamationId = recFound.get().getId();
        System.out.println("✅ Réclamation ajoutée avec ID (idRec) : " + createdReclamationId);
    }

    @Test
    @Order(2)
    @DisplayName("2. Test méthode update()")
    public void testUpdate() {
        Reclamation rUpdate = new Reclamation(
                "PC Réparé",
                "Matériel",
                "Migration Windows",
                "Fermé",
                "2024-05-21"
        );
        rUpdate.setId(createdReclamationId);

        reclamationService.update(rUpdate);

        List<Reclamation> list = reclamationService.getAll();
        Reclamation updated = list.stream()
                .filter(rec -> rec.getId() == createdReclamationId)
                .findFirst()
                .orElse(null);

        assertNotNull(updated);
        assertEquals("PC Réparé", updated.getTitre());
        assertEquals("Fermé", updated.getStatut());
        System.out.println("✅ Update OK.");
    }

    @Test
    @Order(3)
    @DisplayName("Test Explicite getAll()")
    public void testGetAll() {
        List<Reclamation> list = reclamationService.getAll();
        assertNotNull(list, "La liste ne doit pas être null");
        assertFalse(list.isEmpty(), "La liste ne doit pas être vide car on a ajouté une réclamation");
        System.out.println("✅ getAll() fonctionne et retourne " + list.size() + " éléments.");
    }

    @Test
    @Order(4)
    @DisplayName("3. Test méthode getReclamationsByUserId()")
    public void testGetByUserId() {
        List<Reclamation> list = reclamationService.getReclamationsByUserId(currentUserId);
        assertFalse(list.isEmpty());
        boolean hasMyRec = list.stream().anyMatch(r -> r.getId() == createdReclamationId);
        assertTrue(hasMyRec);
        System.out.println("✅ GetByUserId OK.");
    }

    @Test
    @Order(5)
    @DisplayName("4. Test méthode delete()")
    public void testDelete() {
        reclamationService.delete(createdReclamationId);
        List<Reclamation> list = reclamationService.getAll();
        boolean exists = list.stream().anyMatch(rec -> rec.getId() == createdReclamationId);
        assertFalse(exists, "La réclamation ne devrait plus exister");
        System.out.println("✅ Delete OK.");
    }

    @AfterAll
    public static void tearDown() throws SQLException {
        if (currentUserId != 0) {
            userService.supprimerCompte(currentUserId);
            System.out.println("--- NETTOYAGE : User temporaire supprimé ---");
        }
    }
}