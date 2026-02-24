

import com.example.testp1.entities.BudgetProfil;
import com.example.testp1.services.ServiceBudgetProfil;
import java.sql.SQLException;
import java.time.Year;
import java.util.List;

public class CrudTest5 {
    public static void main(String[] args) {
        ServiceBudgetProfil service = new ServiceBudgetProfil();

        try {
            // 1. ADD: Initialize the global fiscal profile for the current year
            System.out.println("--- 1. Testing Initial Add ---");
            BudgetProfil initialProfil = new BudgetProfil(Year.of(2026), 50000.00, 0.00, 12.5f);
            service.add(initialProfil);

            // 2. SINGLETON TEST: Verify that the Service blocks a second insertion
            System.out.println("\n--- 2. Testing Singleton Constraint (One Row Only) ---");
            try {
                BudgetProfil secondProfil = new BudgetProfil(Year.of(2027), 20000.00, 0.00, 10.0f);
                service.add(secondProfil);
            } catch (SQLException e) {
                System.out.println("Constraint Verified: " + e.getMessage());
            }

            // 3. FETCH & UPDATE: Retrieve the active record and modify its values
            System.out.println("\n--- 3. Testing Fetch & Update ---");
            List<BudgetProfil> profiles = service.getAll();
            if (!profiles.isEmpty()) {
                BudgetProfil active = profiles.get(0);
                System.out.println("Current Profile: " + active.toString());

                // Update the disposable amount and profit margin
                active.setBudgetDisposable(85000.00);
                active.setMarginProfit(18.2f);
                service.update(active);

                // Verify the update was persisted
                BudgetProfil updated = service.getAll().get(0);
                System.out.println("Updated Profile: " + updated.toString());
            }

            // 4. DELETE: Clean up the record to reset the test state
            System.out.println("\n--- 4. Testing Delete ---");
            List<BudgetProfil> toDelete = service.getAll();
            if (!toDelete.isEmpty()) {
                service.delete(toDelete.get(0));
                System.out.println("Profile deleted successfully.");
            }

            System.out.println("\n--- Final Check ---");
            System.out.println("Profiles in Database: " + service.getAll().size());

        } catch (SQLException e) {
            System.err.println("Test Failed: " + e.getMessage());
        }
    }
}