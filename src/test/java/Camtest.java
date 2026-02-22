import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import javax.swing.JFrame;

public class Camtest {
    public static void main(String[] args) {
        System.out.println("-> Searching for laptop camera...");

        // 1. Find the default laptop camera
        Webcam webcam = Webcam.getDefault();

        if (webcam == null) {
            System.err.println("-> ERROR: No camera detected by Sarxos!");
            return;
        }

        // 2. Set resolution (VGA is 640x480 - good balance of quality and speed)
        webcam.setViewSize(WebcamResolution.VGA.getSize());

        // 3. Create Sarxos's built-in video panel
        WebcamPanel panel = new WebcamPanel(webcam);
        panel.setFPSDisplayed(true);       // Show frames per second on screen
        panel.setImageSizeDisplayed(true); // Show resolution on screen
        panel.setMirrored(true);           // Makes it act like a mirror

        // 4. Throw it in a basic window and show it
        JFrame window = new JFrame("Hardware Camera Test");
        window.add(panel);
        window.setResizable(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.pack();
        window.setLocationRelativeTo(null); // Center on screen
        window.setVisible(true);

        System.out.println("-> Camera is LIVE. Close the window to shut it off.");
    }
}