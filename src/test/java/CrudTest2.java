
import com.example.testp1.entities.ProjectBudget;
import com.example.testp1.services.ServiceProjectBudget;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;


public class CrudTest2 {

    public static class BudgetLifecycleTest {
        public static void main(String[] args) {
            ServiceProjectBudget service = new ServiceProjectBudget();

            try {

                ProjectBudget newBudget = new ProjectBudget("Auto Portal v2", 8000.0, 0.0, "ON TRACK", LocalDate.of(2026, 4, 10), 3);
                service.add(newBudget);


                List<ProjectBudget> all = service.getAll();
                ProjectBudget retrieved = all.get(all.size() - 1);
                int generatedId = retrieved.getId();
                System.out.println("Budget added with Auto-Increment ID: " + generatedId);


                System.out.println("\n--- Testing Update ---");
                ProjectBudget budgetToUpdate = new ProjectBudget(generatedId, "Client Portal v2", 9500.0, 500.0, "ON TRACK", retrieved.getDueDate(), 3);
                service.update(budgetToUpdate);


                System.out.println("\n--- Testing Delete ---");
                service.delete(budgetToUpdate);

                System.out.println("Final Verification: Budget count is " + service.getAll().size());

            } catch (SQLException e) {
                System.err.println("Test Error: " + e.getMessage());
            }
        }
    }
}
