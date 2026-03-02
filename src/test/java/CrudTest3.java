
import com.example.testp1.entities.Transaction;
import com.example.testp1.services.ServiceTransaction;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class CrudTest3 {
    public static void main(String[] args) {
        ServiceTransaction service = new ServiceTransaction();

        try {
            // 1. ADD: Create a new transaction with a unique reference
            System.out.println("--- 1. Testing Add ---");
            String uniqueRef = "TX-" + System.currentTimeMillis(); // Simple unique ref generator
            Transaction newTx = new Transaction(uniqueRef, 450.75, LocalDate.now(), "Hardware", 1, 101);
            service.add(newTx);


            System.out.println("\n--- 2. Testing Fetch (GetAll) ---");
            List<Transaction> allTransactions = service.getAll();
            Transaction latest = allTransactions.get(allTransactions.size() - 1);
            System.out.println("Latest Transaction: " + latest.toString());
            int generatedId = latest.getId();


            System.out.println("\n--- 3. Testing Update for ID: " + generatedId + " ---");
            Transaction txToUpdate = new Transaction(
                    generatedId,
                    latest.getReference(),
                    600.00,
                    latest.getDateStamp(),
                    "Infrastructure",
                    1,
                    101
            );
            service.update(txToUpdate);
            System.out.println("Updated logic executed.");

            System.out.println("\n--- 4. Testing Delete ---");
           // service.delete(txToUpdate);


            System.out.println("\n--- Final Verification ---");
            System.out.println("Transactions remaining in DB: " + service.getAll().size());

        } catch (SQLException e) {
            System.err.println("Transaction CRUD Test Failed: " + e.getMessage());
        }
    }
}