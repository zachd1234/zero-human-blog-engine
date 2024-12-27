package com.zho.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    public static final String URL = "jdbc:mysql://localhost:3306/autoblog1";
    public static final String USER = "zach_derhake";
    public static final String PASSWORD = "***REMOVED***";
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("Database connected successfully!");
            System.out.println("Connection valid: " + !conn.isClosed());
        } catch (SQLException e) {
            System.out.println("Database connection failed!");
            e.printStackTrace();
        }
    }
} 