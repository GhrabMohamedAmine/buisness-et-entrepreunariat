

import com.example.testp1.entities.ProjectBudget;
import com.example.testp1.services.ServiceProjectBudget;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
public class CrudTest {
    public static void main(String[] args) {
        ServiceProjectBudget service = new ServiceProjectBudget();

        try {
            // 1. Create a new budget for 'Nexum Core Dashboard' (ID: 1)
            ProjectBudget newBudget = new ProjectBudget();
            newBudget.setName("Nexum Core Dashboard");
            newBudget.setProjectId(1); // Link to existing project
            newBudget.setTotalBudget(5000.00);
            newBudget.setActualSpend(0.00);
            newBudget.setStatus("ON TRACK");
            newBudget.setDueDate(LocalDate.of(2026, 5, 30));

            System.out.println("--- Testing Add ---");
            service.add(newBudget);

            // 2. Fetch all budgets to verify the insertion
            System.out.println("\n--- Testing Fetch (getAll) ---");
            List<ProjectBudget> budgets = service.getAll();

            for (ProjectBudget b : budgets) {
                System.out.println(b.toString());
            }

        } catch (SQLException e) {
            System.err.println("Test Failed: " + e.getMessage());
        }
    }
}
