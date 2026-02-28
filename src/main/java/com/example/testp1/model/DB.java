package com.example.testp1.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {

    private final String URL = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:8889/Nexum");
    private final String USERNAME = System.getenv().getOrDefault("DB_USERNAME", "root");
    private final String PWD = System.getenv().getOrDefault("DB_PASSWORD", "root");

    private Connection conx;

    public static DB instance;

    private DB(){
        try {
            conx = DriverManager.getConnection(URL,USERNAME,PWD);
            System.out.println("Connexion établie!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static DB getInstance(){
        if (instance == null){
            instance = new DB();
        }
        return instance;

    }


    public Connection getConx() {
        return conx;
    }
}
