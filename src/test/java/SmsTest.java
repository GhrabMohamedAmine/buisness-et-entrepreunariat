import services.notifications.SmsService;

public class SmsTest {
    // Drop this right inside the SmsService class
    public static void main(String[] args) {
        System.out.println("Starting SMS Test...");
        SmsService testService = new SmsService();

        // ⚠️ REPLACE THIS with your actual verified mobile number!
        // Make sure to include the country code (e.g., +216 for Tunisia)
        String myRealNumber = "+21629335148";

        String testMessage = "Hello from Nexum! If you receive this, the Twilio keys are perfectly configured.";

        try {
            System.out.println("Attempting to send message to: " + myRealNumber);
            testService.sendSms(myRealNumber, testMessage);
            System.out.println("✅ SUCCESS! The SMS was accepted by Twilio.");
        } catch (Exception e) {
            System.err.println("❌ FAILED! Twilio rejected the request.");
            e.printStackTrace();
        }
    }
}
