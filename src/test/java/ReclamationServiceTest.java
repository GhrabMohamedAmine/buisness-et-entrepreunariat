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

    // On doit stocker les IDs pour enchaîner les tests
    private static int currentUserId;
    private static int createdReclamationId;

    // Email unique pour éviter les conflits
    private static final String TEST_EMAIL = "test_rec_" + System.currentTimeMillis() + "@gmail.com";

    @BeforeAll
    public static void setup() throws SQLException {
        reclamationService = new ReclamationService();
        userService = new UserService();

        System.out.println("--- PRÉPARATION : Création et Connexion User ---");

        // 1. Création d'un User de test
        User user = new User(0, "Tester", "Reclam", TEST_EMAIL, "00000000",
                "Client", "IT", "Actif", "2024-01-01", "img.png");

        // 2. ÉTAPE DE SIGNUP : On insère le user en base de données
        // On utilise la nouvelle méthode 'signup' (qui ne connecte plus l'utilisateur)
        userService.signup(user, "password123");

        // 3. ÉTAPE DE LOGIN : On authentifie manuellement pour définir le 'currentUser'
        // C'est indispensable car 'add' dépend de UserService.getCurrentUser()
        boolean loginSuccess = userService.authenticate(TEST_EMAIL, "password123");
        assertTrue(loginSuccess, "L'authentification doit réussir après le signup !");

        // 4. On récupère l'ID du user désormais connecté
        User connectedUser = UserService.getCurrentUser();
        assertNotNull(connectedUser, "Le user doit être connecté pour tester les réclamations !");

        currentUserId = connectedUser.getId();
        System.out.println("✅ User enregistré et connecté avec succès. ID : " + currentUserId);
    }

    @Test
    @Order(1)
    @DisplayName("1. Test méthode add()")
    public void testAdd() {
        // Création de l'objet (sans ID ni UserID, car gérés par la base et le service)
        Reclamation r = new Reclamation(
                "PC en panne",          // titre
                "Matériel",             // categorie
                "Migration Windows",    // projet
                "En cours",             // statut
                "2024-05-20"            // date
        );

        // Appel de la méthode add
        // Elle va utiliser UserService.getCurrentUser() en interne
        reclamationService.add(r);

        // Vérification
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
        // On crée un objet avec les nouvelles infos
        Reclamation rUpdate = new Reclamation(
                "PC Réparé",            // Nouveau titre
                "Matériel",
                "Migration Windows",
                "Fermé",                // Nouveau statut
                "2024-05-21"
        );

        // IMPORTANT : On doit donner l'ID de la réclamation à modifier
        rUpdate.setId(createdReclamationId);

        // Appel de la méthode update
        reclamationService.update(rUpdate);

        // Vérification via getAll()
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
    @Order(3) // Vous pouvez décaler les numéros d'ordre des tests suivants
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
        // Appel de la méthode getReclamationsByUserId
        List<Reclamation> list = reclamationService.getReclamationsByUserId(currentUserId);

        assertFalse(list.isEmpty());
        // Vérifie qu'on a bien au moins notre réclamation
        boolean hasMyRec = list.stream().anyMatch(r -> r.getId() == createdReclamationId);
        assertTrue(hasMyRec);

        System.out.println("✅ GetByUserId OK.");
    }

    @Test
    @Order(5)
    @DisplayName("4. Test méthode delete()")
    public void testDelete() {
        // Appel de la méthode delete
        reclamationService.delete(createdReclamationId);

        // Vérification qu'elle n'existe plus
        List<Reclamation> list = reclamationService.getAll();
        boolean exists = list.stream().anyMatch(rec -> rec.getId() == createdReclamationId);

        assertFalse(exists, "La réclamation ne devrait plus exister");
        System.out.println("✅ Delete OK.");
    }

    @AfterAll
    public static void tearDown() throws SQLException {
        // Nettoyage de la base de données (User)
        if (currentUserId != 0) {
            userService.supprimerCompte(currentUserId);
            System.out.println("--- NETTOYAGE : User temporaire supprimé ---");
        }
    }
}