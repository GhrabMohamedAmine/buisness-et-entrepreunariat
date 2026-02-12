package tezfx.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import java.io.IOException;

public class ViewLoader {
    public static Node load(String fxmlPath) {
        try {
            String resolvedPath = resolvePath(fxmlPath);
            if (resolvedPath == null) {
                System.err.println("FXML not found: " + fxmlPath);
                return null;
            }
            return new FXMLLoader(ViewLoader.class.getResource(resolvedPath)).load();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String resolvePath(String fxmlPath) {
        if (fxmlPath == null || fxmlPath.isBlank()) {
            return null;
        }
        if (fxmlPath.startsWith("/")) {
            return fxmlPath;
        }
        String baseViewPath = "/tezfx/view";
        String candidate = baseViewPath + "/" + fxmlPath;
        if (ViewLoader.class.getResource(candidate) != null) {
            return candidate;
        }
        return ViewLoader.class.getResource("/" + fxmlPath) != null ? "/" + fxmlPath : null;
    }
}
