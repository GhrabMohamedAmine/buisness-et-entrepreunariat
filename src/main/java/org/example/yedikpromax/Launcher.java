package org.example.yedikpromax;
import utils.DBConnection;

import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        try {
            System.out.println("Connected: " + DBConnection.getInstance().getConnection());
            System.out.println(System.getProperty("os.arch"));
            System.out.println(System.getProperty("sun.arch.data.model"));

        } catch (Exception e) {
            e.printStackTrace();
        }
        Application.launch(HelloApplication.class, args);
    }
}
