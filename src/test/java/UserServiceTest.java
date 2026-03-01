import entities.User;
import org.junit.jupiter.api.*;
import services.UserService;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserServiceTest {

    private static UserService userService;
    private static int createdUserId;

    // Email unique pour ce lancement de test
    private static final String TEST_EMAIL = "test_full_" + System.currentTimeMillis() + "@gmail.com";

    @BeforeAll
    public static void setup() {
        userService = new UserService();
        System.out.println(">>> Démarrage de la suite de tests complète <<<");
    }

    @Test
    @Order(1)
    @DisplayName("1. Inscription (Création)")
    public void testSignup() throws SQLException {
        // Utilisation du constructeur complet avec 11 paramètres
        // Les champs byte[] et String (faceId) sont mis à null (pas d'image ni de faceId pour ce test)
        User newUser = new User(
                0,                           // id (sera généré)
                "Wayne",                      // nom
                "Bruce",                      // prénom
                TEST_EMAIL,                   // email
                "99999999",                    // téléphone
                "Client",                      // rôle
                "IT",                          // département
                "Actif",                       // statut
                "2024-01-01",                  // date d'inscription
                null,                          // imageData (byte[])
                null                           // faceId
        );

        userService.signup(newUser, "password123");

        // Vérification via recupererTous
        List<User> users = userService.recupererTous();
        Optional<User> userFound = users.stream()
                .filter(u -> u.getEmail().equals(TEST_EMAIL))
                .findFirst();

        assertTrue(userFound.isPresent(), "L'inscription a échoué (User non trouvé)");

        createdUserId = userFound.get().getId();
        assertNull(UserService.getCurrentUser(), "Le user ne devrait pas être connecté automatiquement après le signup");

        System.out.println("✅ Inscription réussie en base de données. ID = " + createdUserId);
    }

    @Test
    @Order(2)
    @DisplayName("2. Authentification (Login)")
    public void testAuthenticate() throws SQLException {
        // Test Auth Réussie
        boolean success = userService.authenticate(TEST_EMAIL, "password123");
        assertTrue(success, "Login échoué avec le bon mot de passe");

        User current = UserService.getCurrentUser();
        assertNotNull(current, "La session (currentUser) est vide après login");
        assertEquals(createdUserId, current.getId(), "L'ID du currentUser est incorrect");

        // Test Auth Echouée
        boolean fail = userService.authenticate(TEST_EMAIL, "mauvaisPass");
        assertFalse(fail, "Login réussi avec un mauvais mot de passe (Anormal)");
        System.out.println("✅ Authentification OK.");
    }

    @Test
    @Order(3)
    @DisplayName("3. Modification du Profil")
    public void testModifierProfil() throws SQLException {
        // Création d'un utilisateur avec les nouvelles données
        // On conserve le même email, on change nom, prénom, téléphone, département
        // imageData = null (pas de changement d'image)
        User userToUpdate = new User(
                createdUserId,
                "Kent",                        // nouveau nom
                "Clark",                       // nouveau prénom
                TEST_EMAIL,                    // email inchangé
                "11111111",                     // nouveau téléphone
                "Client",                       // rôle (inchangé)
                "Journalism",                    // nouveau département
                "Actif",                         // statut
                "2024-01-01",                    // date inchangée
                null,                            // imageData (pas de changement)
                null                             // faceId (inchangé)
        );

        userService.modifierProfil(userToUpdate);

        // Vérification
        User updatedUser = getUserFromDb(createdUserId);
        assertEquals("Kent", updatedUser.getName(), "Nom non mis à jour");
        assertEquals("Clark", updatedUser.getFirstName(), "Prénom non mis à jour");
        assertEquals("11111111", updatedUser.getPhone(), "Téléphone non mis à jour");
        assertEquals("Journalism", updatedUser.getDepartment(), "Département non mis à jour");
        // Vérifier que l'image est toujours null (ou inchangée)
        assertNull(updatedUser.getImageData(), "L'image devrait être null");
        System.out.println("✅ Modification Profil OK.");
    }

    @Test
    @Order(4)
    @DisplayName("4. Modification du Rôle (Admin)")
    public void testModifierRole() throws SQLException {
        // Vérifions d'abord que la méthode existe (elle est présente dans votre UserService)
        // Si la méthode n'existe pas, ce test échouera (vous pouvez l'adapter)
        userService.modifierRole(createdUserId, "Admin");

        User updatedUser = getUserFromDb(createdUserId);
        assertEquals("Admin", updatedUser.getRole(), "Le rôle n'a pas été modifié");
        System.out.println("✅ Modification Rôle OK.");
    }

    @Test
    @Order(5)
    @DisplayName("5. Récupérer Tous (Liste)")
    public void testRecupererTous() throws SQLException {
        List<User> users = userService.recupererTous();
        assertNotNull(users, "La liste ne doit pas être null");
        assertFalse(users.isEmpty(), "La liste ne doit pas être vide");

        boolean present = users.stream().anyMatch(u -> u.getId() == createdUserId);
        assertTrue(present, "L'utilisateur de test est introuvable dans la liste globale");
        System.out.println("✅ Récupération Liste OK (" + users.size() + " utilisateurs trouvés).");
    }

    @Test
    @Order(6)
    @DisplayName("6. Suppression Compte")
    public void testSupprimerCompte() throws SQLException {
        userService.supprimerCompte(createdUserId);

        User deletedUser = getUserFromDb(createdUserId);
        assertNull(deletedUser, "L'utilisateur existe encore après suppression !");

        assertNull(UserService.getCurrentUser(), "La session n'a pas été nettoyée après suppression");
        System.out.println("✅ Suppression OK.");
    }

    // --- Méthode utilitaire pour éviter de répéter le code de recherche ---
    private User getUserFromDb(int id) throws SQLException {
        return userService.recupererTous().stream()
                .filter(u -> u.getId() == id)
                .findFirst()
                .orElse(null);
    }
}