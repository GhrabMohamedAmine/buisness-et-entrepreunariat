package com.example.testp1.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {

    private final String URL ="jdbc:mysql://localhost:3306/nexum";
    private final String USERNAME ="root";
    private final String PWD ="";

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