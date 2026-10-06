package com.studentmanagement;

import com.studentmanagement.config.DatabaseConnection;
import com.studentmanagement.web.WebServer;

import java.awt.Desktop;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Scanner;

/**
 * Main.java
 * ==========
 * PURPOSE: Application entry point.
 * Launches the Student Management System Web Portal on localhost.
 */
public class Main {

    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        printBanner();

        // 1. Verify Database Connection
        System.out.println("Checking MySQL database connection...");
        if (!DatabaseConnection.testConnection()) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("  [✗] Cannot connect to MySQL database!");
            System.out.println("=".repeat(60));
            System.out.println("  Please check:");
            System.out.println("    1. MySQL server is running");
            System.out.println("    2. Database 'student_db' exists");
            System.out.println("    3. Password in DatabaseConnection.java is correct");
            System.out.println("=".repeat(60));
            return;
        }
        System.out.println("[✓] Connected to MySQL database (student_db)\n");

        // 2. Select Available Port
        int port = getAvailablePort(DEFAULT_PORT);

        // 3. Start Web Server
        WebServer webServer = new WebServer(port);
        try {
            webServer.start();
        } catch (IOException e) {
            System.err.println("[✗] Failed to start web server: " + e.getMessage());
            return;
        }

        String url = "http://localhost:" + port;
        printServerReady(url);

        // 4. Try opening the browser automatically
        tryOpenBrowser(url);

        // 5. Keep server running and provide interactive CLI controls
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nCommands:");
        System.out.println("  [b] Open in browser");
        System.out.println("  [q] Stop and exit");
        System.out.println("=".repeat(60));

        while (true) {
            System.out.print("\nWeb Server is LIVE > ");
            if (!scanner.hasNextLine()) {
                // If running in background without input stream, block thread
                try {
                    Thread.currentThread().join();
                } catch (InterruptedException ignored) {}
                break;
            }

            String cmd = scanner.nextLine().trim().toLowerCase();
            if ("q".equals(cmd) || "exit".equals(cmd)) {
                System.out.println("Shutting down Web Server...");
                webServer.stop();
                System.out.println("Server stopped. Goodbye!");
                break;
            } else if ("b".equals(cmd) || "open".equals(cmd)) {
                tryOpenBrowser(url);
            } else {
                System.out.println("Server running at " + url + " - enter 'b' to open browser, 'q' to quit.");
            }
        }
    }

    private static int getAvailablePort(int defaultPort) {
        try (ServerSocket socket = new ServerSocket(defaultPort)) {
            return defaultPort;
        } catch (IOException e) {
            // If default port is busy, fallback to next port
            try (ServerSocket socket2 = new ServerSocket(0)) {
                return socket2.getLocalPort();
            } catch (IOException ignored) {
                return defaultPort;
            }
        }
    }

    private static void tryOpenBrowser(String url) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("Opening browser at " + url + " ...");
            } catch (Exception ignored) {
                // Headless or permission restricted, continue
            }
        }
    }

    private static void printBanner() {
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                  ║");
        System.out.println("║          🎓 STUDENT MANAGEMENT SYSTEM - WEB PORTAL               ║");
        System.out.println("║          ─────────────────────────────────────────               ║");
        System.out.println("║          Built with Java + JDBC + MySQL                          ║");
        System.out.println("║                                                                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
    }

    private static void printServerReady(String url) {
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                  ║");
        System.out.println("║   🚀 WEB PORTAL IS READY AND RUNNING!                            ║");
        System.out.println("║                                                                  ║");
        System.out.println("║   👉 Open your browser at:                                       ║");
        System.out.println("║      " + String.format("%-60s", url) + "║");
        System.out.println("║                                                                  ║");
        System.out.println("║   🗄️  Database : MySQL (student_db on localhost:3306)            ║");
        System.out.println("║   ⚡ Tech     : Pure Java, Core JDBC, REST API & Modern UI       ║");
        System.out.println("║                                                                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
    }
}
