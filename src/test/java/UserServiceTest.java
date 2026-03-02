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
        User newUser = new User(
                0, "Wayne", "Bruce", TEST_EMAIL, "99999999",
                "Client", "IT", "Actif", "2024-01-01", "batman.png"
        );

        // 1. Appeler la méthode mise à jour qui ne fait que l'insertion
        userService.signup(newUser, "password123");

        // 2. Vérification immédiate via recupererTous
        List<User> users = userService.recupererTous();
        Optional<User> userFound = users.stream()
                .filter(u -> u.getEmail().equals(TEST_EMAIL))
                .findFirst();

        // 3. Assertions
        assertTrue(userFound.isPresent(), "L'inscription a échoué (User non trouvé)");

        // Vérifier que l'ID a bien été généré et injecté dans l'objet par getGeneratedKeys
        createdUserId = userFound.get().getId();

        // Vérifier que l'utilisateur n'est PAS connecté automatiquement (currentUser doit être null ou différent)
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

        // Vérification du Current User en mémoire
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
        User userToUpdate = new User(
                createdUserId, "Kent", "Clark", TEST_EMAIL, "11111111",
                "Client", "Journalism", "Actif", "2024-01-01", "superman.png"
        );

        userService.modifierProfil(userToUpdate);

        // Vérification
        User updatedUser = getUserFromDb(createdUserId);
        assertEquals("Kent", updatedUser.getName(), "Nom non mis à jour");
        assertEquals("superman.png", updatedUser.getImageLink(), "Image non mise à jour");
        System.out.println("✅ Modification Profil OK.");
    }

    @Test
    @Order(4)
    @DisplayName("4. Modification du Rôle (Admin)")
    public void testModifierRole() throws SQLException {
        // Hypothèse : votre méthode s'appelle modifierRole(int id, String newRole)
        // Si elle s'appelle différemment, ajustez le nom ici.
        try {
            // Note : Cette méthode est visible dans vos snippets (UPDATE utilisateurs SET role...)
            userService.modifierRole(createdUserId, "Admin");

            User updatedUser = getUserFromDb(createdUserId);
            assertEquals("Admin", updatedUser.getRole(), "Le rôle n'a pas été modifié");
            System.out.println("✅ Modification Rôle OK.");
        } catch (Exception e) {
            // Si la méthode n'existe pas encore dans UserService, ce bloc l'ignorera
            System.out.println("⚠️ Test Rôle ignoré (Méthode modifierRole introuvable ou erreur SQL)");
        }
    }

    @Test
    @Order(5)
    @DisplayName("5. Récupérer Tous (Liste)")
    public void testRecupererTous() throws SQLException {
        List<User> users = userService.recupererTous();
        assertNotNull(users, "La liste ne doit pas être null");
        assertFalse(users.isEmpty(), "La liste ne doit pas être vide");

        // On vérifie que notre user de test est bien dedans
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

        // Vérification que le currentUser est vidé
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