package com.studentmanagement.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseConnection.java
 * ========================
 * PURPOSE: Manages the JDBC connection to the MySQL database.
 *
 * WHY THIS CLASS EXISTS:
 *   Instead of creating database connections everywhere in the code,
 *   we centralize it here. Any class that needs a database connection
 *   simply calls DatabaseConnection.getConnection().
 *
 * JDBC CONCEPTS DEMONSTRATED:
 *   1. Loading the JDBC Driver (mysql-connector-j)
 *   2. DriverManager.getConnection() — creates a connection to MySQL
 *   3. Connection object — represents an active session with the database
 */
public class DatabaseConnection {

    // Database connection details
    // These can be overridden using environment variables:
    //   DB_URL, DB_USER, DB_PASSWORD
    private static final String DEFAULT_URL  = "jdbc:mysql://localhost:3306/student_db";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "root8989";

    /**
     * Returns a new Connection to the MySQL database.
     *
     * HOW IT WORKS:
     *   1. Checks for environment variables first (DB_URL, DB_USER, DB_PASSWORD)
     *   2. Falls back to default values if env vars are not set
     *   3. Uses DriverManager.getConnection() to establish a JDBC connection
     *
     * IMPORTANT:
     *   - Each call returns a NEW connection
     *   - The caller is responsible for CLOSING the connection (use try-with-resources)
     *   - If MySQL is not running, this will throw a SQLException
     *
     * @return Connection object connected to student_db
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        // Read from environment variables, or use defaults
        String url  = System.getenv("DB_URL")      != null ? System.getenv("DB_URL")      : DEFAULT_URL;
        String user = System.getenv("DB_USER")     != null ? System.getenv("DB_USER")     : DEFAULT_USER;
        String pass = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : DEFAULT_PASS;

        /*
         * DriverManager.getConnection(url, user, password)
         * ================================================
         * This is the core JDBC method that:
         *   1. Finds the appropriate JDBC driver (MySQL Connector/J)
         *   2. Establishes a TCP connection to the MySQL server
         *   3. Authenticates with the given username and password
         *   4. Returns a Connection object for executing SQL
         */
        return DriverManager.getConnection(url, user, pass);
    }

    /**
     * Tests whether the database connection is working.
     * Called at application startup to verify MySQL is accessible.
     *
     * @return true if connection is successful, false otherwise
     */
    public static boolean testConnection() {
        // try-with-resources: automatically closes the connection after the block
        try (Connection connection = getConnection()) {
            // connection.isValid(timeout) checks if the connection is alive
            return connection != null && connection.isValid(5);
        } catch (SQLException e) {
            System.err.println("[ERROR] Database connection failed: " + e.getMessage());
            return false;
        }
    }
}
