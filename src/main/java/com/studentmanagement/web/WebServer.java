package com.studentmanagement.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.studentmanagement.config.DatabaseConnection;
import com.studentmanagement.dao.StudentDAO;
import com.studentmanagement.model.Student;
import com.studentmanagement.util.InputValidator;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * WebServer.java
 * ==============
 * PURPOSE: Lightweight built-in HTTP server using standard JDK (com.sun.net.httpserver).
 *
 * Provides:
 *   1. Full REST API for Student CRUD operations
 *   2. Modern Static Web UI Serving (HTML / CSS / JS)
 *   3. CORS support for local web access
 */
public class WebServer {

    private final int port;
    private final StudentDAO studentDAO;
    private final Gson gson;
    private HttpServer server;

    public WebServer(int port) {
        this.port = port;
        this.studentDAO = new StudentDAO();
        this.gson = new Gson();
    }

    /**
     * Starts the HTTP server.
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // API routes
        server.createContext("/api/health", new HealthHandler());
        server.createContext("/api/stats", new StatsHandler());
        server.createContext("/api/students", new StudentsHandler());

        // Static files (Web portal frontend)
        server.createContext("/", new StaticFileHandler());

        // Multi-threaded executor
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("[✓] Web Server started on port " + port);
    }

    /**
     * Stops the HTTP server.
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("[✓] Web Server stopped.");
        }
    }

    // ====================================================================
    //                         HANDLERS
    // ====================================================================

    /**
     * Health check endpoint: GET /api/health
     */
    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            boolean dbConnected = DatabaseConnection.testConnection();
            JsonObject response = new JsonObject();
            response.addProperty("status", dbConnected ? "UP" : "DOWN");
            response.addProperty("database", dbConnected ? "Connected" : "Disconnected");

            sendJsonResponse(exchange, dbConnected ? 200 : 503, response.toString());
        }
    }

    /**
     * Stats endpoint: GET /api/stats
     */
    private class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendErrorResponse(exchange, 405, "Method not allowed");
                return;
            }

            List<Student> students = studentDAO.getAllStudents();
            int total = students.size();

            Set<String> departments = new HashSet<>();
            Set<String> courses = new HashSet<>();
            Map<String, Integer> deptCounts = new HashMap<>();
            Map<String, Integer> genderCounts = new HashMap<>();

            for (Student s : students) {
                if (s.getDepartment() != null && !s.getDepartment().isBlank()) {
                    departments.add(s.getDepartment());
                    deptCounts.put(s.getDepartment(), deptCounts.getOrDefault(s.getDepartment(), 0) + 1);
                }
                if (s.getCourse() != null && !s.getCourse().isBlank()) {
                    courses.add(s.getCourse());
                }
                if (s.getGender() != null && !s.getGender().isBlank()) {
                    genderCounts.put(s.getGender(), genderCounts.getOrDefault(s.getGender(), 0) + 1);
                }
            }

            JsonObject stats = new JsonObject();
            stats.addProperty("totalStudents", total);
            stats.addProperty("totalDepartments", departments.size());
            stats.addProperty("totalCourses", courses.size());
            stats.add("departments", gson.toJsonTree(deptCounts));
            stats.add("gender", gson.toJsonTree(genderCounts));

            sendJsonResponse(exchange, 200, stats.toString());
        }
    }

    /**
     * Students CRUD Handler:
     *   GET    /api/students           -> List all or search
     *   GET    /api/students/{id}      -> Get by ID
     *   POST   /api/students           -> Add student
     *   PUT    /api/students/{id}      -> Update student
     *   DELETE /api/students/{id}      -> Delete student
     */
    private class StudentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();

            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath(); // e.g., /api/students or /api/students/5
            String[] parts = path.split("/");

            Integer studentId = null;
            if (parts.length >= 4 && !parts[3].isEmpty()) {
                try {
                    studentId = Integer.parseInt(parts[3]);
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Invalid student ID in path");
                    return;
                }
            }

            switch (method) {
                case "GET":
                    if (studentId != null) {
                        handleGetById(exchange, studentId);
                    } else {
                        handleGetAllOrSearch(exchange);
                    }
                    break;

                case "POST":
                    handleCreateStudent(exchange);
                    break;

                case "PUT":
                    if (studentId == null) {
                        sendErrorResponse(exchange, 400, "Student ID required for update");
                        return;
                    }
                    handleUpdateStudent(exchange, studentId);
                    break;

                case "DELETE":
                    if (studentId == null) {
                        sendErrorResponse(exchange, 400, "Student ID required for delete");
                        return;
                    }
                    handleDeleteStudent(exchange, studentId);
                    break;

                default:
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                    break;
            }
        }

        private void handleGetAllOrSearch(HttpExchange exchange) throws IOException {
            Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getRawQuery());
            String keyword = queryParams.get("keyword");
            String field = queryParams.get("field");

            List<Student> students;
            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchField = (field != null && !field.trim().isEmpty()) ? field.trim() : "name";
                students = studentDAO.searchStudents(searchField, keyword.trim());
            } else {
                students = studentDAO.getAllStudents();
            }

            sendJsonResponse(exchange, 200, gson.toJson(students));
        }

        private void handleGetById(HttpExchange exchange, int id) throws IOException {
            Student student = studentDAO.getStudentById(id);
            if (student == null) {
                sendErrorResponse(exchange, 404, "Student not found with ID: " + id);
            } else {
                sendJsonResponse(exchange, 200, gson.toJson(student));
            }
        }

        private void handleCreateStudent(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            Student student;
            try {
                student = gson.fromJson(body, Student.class);
            } catch (Exception e) {
                sendErrorResponse(exchange, 400, "Malformed JSON request");
                return;
            }

            if (student == null) {
                sendErrorResponse(exchange, 400, "Empty student payload");
                return;
            }

            // Validation
            String valError = validateStudent(student);
            if (valError != null) {
                sendErrorResponse(exchange, 400, valError);
                return;
            }

            // Check email uniqueness
            if (studentDAO.emailExists(student.getEmail())) {
                sendErrorResponse(exchange, 409, "A student with email '" + student.getEmail() + "' already exists");
                return;
            }

            boolean added = studentDAO.addStudent(student);
            if (added) {
                JsonObject res = new JsonObject();
                res.addProperty("success", true);
                res.addProperty("message", "Student created successfully");
                sendJsonResponse(exchange, 201, res.toString());
            } else {
                sendErrorResponse(exchange, 500, "Database error: Could not save student");
            }
        }

        private void handleUpdateStudent(HttpExchange exchange, int id) throws IOException {
            Student existing = studentDAO.getStudentById(id);
            if (existing == null) {
                sendErrorResponse(exchange, 404, "Student not found with ID: " + id);
                return;
            }

            String body = readRequestBody(exchange);
            Student updateData;
            try {
                updateData = gson.fromJson(body, Student.class);
            } catch (Exception e) {
                sendErrorResponse(exchange, 400, "Malformed JSON payload");
                return;
            }

            if (updateData == null) {
                sendErrorResponse(exchange, 400, "Empty update payload");
                return;
            }

            updateData.setId(id);
            String valError = validateStudent(updateData);
            if (valError != null) {
                sendErrorResponse(exchange, 400, valError);
                return;
            }

            // Check email collision
            if (studentDAO.emailExistsForOtherStudent(updateData.getEmail(), id)) {
                sendErrorResponse(exchange, 409, "Email '" + updateData.getEmail() + "' is already in use by another student");
                return;
            }

            boolean updated = studentDAO.updateStudent(updateData);
            if (updated) {
                JsonObject res = new JsonObject();
                res.addProperty("success", true);
                res.addProperty("message", "Student updated successfully");
                sendJsonResponse(exchange, 200, res.toString());
            } else {
                sendErrorResponse(exchange, 500, "Database error: Failed to update student");
            }
        }

        private void handleDeleteStudent(HttpExchange exchange, int id) throws IOException {
            Student existing = studentDAO.getStudentById(id);
            if (existing == null) {
                sendErrorResponse(exchange, 404, "Student not found with ID: " + id);
                return;
            }

            boolean deleted = studentDAO.deleteStudent(id);
            if (deleted) {
                JsonObject res = new JsonObject();
                res.addProperty("success", true);
                res.addProperty("message", "Student ID " + id + " deleted successfully");
                sendJsonResponse(exchange, 200, res.toString());
            } else {
                sendErrorResponse(exchange, 500, "Database error: Failed to delete student");
            }
        }

        private String validateStudent(Student s) {
            if (s.getName() == null || !InputValidator.isValidName(s.getName())) {
                return "Invalid name. Must be 2-100 characters letters only.";
            }
            if (s.getEmail() == null || !InputValidator.isValidEmail(s.getEmail())) {
                return "Invalid email format.";
            }
            if (s.getPhone() == null || !InputValidator.isValidPhone(s.getPhone())) {
                return "Invalid phone number (10-15 digits required).";
            }
            if (!InputValidator.isValidAge(s.getAge())) {
                return "Invalid age. Must be between 15 and 60.";
            }
            if (s.getGender() == null || !InputValidator.isValidGender(s.getGender())) {
                return "Invalid gender. Must be Male, Female, or Other.";
            }
            if (s.getCourse() == null || s.getCourse().trim().isEmpty()) {
                return "Course cannot be empty.";
            }
            if (s.getDepartment() == null || s.getDepartment().trim().isEmpty()) {
                return "Department cannot be empty.";
            }
            if (!InputValidator.isValidSemester(s.getSemester())) {
                return "Invalid semester. Must be between 1 and 8.";
            }
            if (s.getAddress() == null || s.getAddress().trim().isEmpty()) {
                return "Address cannot be empty.";
            }
            return null;
        }
    }

    /**
     * Static file handler to serve modern web dashboard (HTML, CSS, JS).
     */
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Sanitize against directory traversal
            path = path.replace("\\", "/");
            if (path.contains("..")) {
                sendErrorResponse(exchange, 403, "Forbidden");
                return;
            }

            String resourcePath = "/web" + path;
            InputStream is = getClass().getResourceAsStream(resourcePath);

            // Fallback: If not in JAR classpath, look directly in src/main/resources/web
            if (is == null) {
                File file = new File("src/main/resources" + resourcePath);
                if (file.exists() && file.isFile()) {
                    is = new FileInputStream(file);
                }
            }

            if (is == null) {
                // If SPA route fallback or file not found
                if (!path.startsWith("/api/")) {
                    InputStream indexIs = getClass().getResourceAsStream("/web/index.html");
                    if (indexIs == null) {
                        File indexFile = new File("src/main/resources/web/index.html");
                        if (indexFile.exists()) {
                            indexIs = new FileInputStream(indexFile);
                        }
                    }
                    if (indexIs != null) {
                        byte[] bytes = indexIs.readAllBytes();
                        indexIs.close();
                        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                        exchange.sendResponseHeaders(200, bytes.length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(bytes);
                        os.close();
                        return;
                    }
                }
                sendErrorResponse(exchange, 404, "File not found: " + path);
                return;
            }

            byte[] bytes = is.readAllBytes();
            is.close();

            String contentType = getContentType(path);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }

        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html; charset=UTF-8";
            if (path.endsWith(".css"))  return "text/css; charset=UTF-8";
            if (path.endsWith(".js"))   return "application/javascript; charset=UTF-8";
            if (path.endsWith(".json")) return "application/json; charset=UTF-8";
            if (path.endsWith(".png"))  return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".svg"))  return "image/svg+xml";
            if (path.endsWith(".ico"))  return "image/x-icon";
            return "text/plain; charset=UTF-8";
        }
    }

    // ====================================================================
    //                       UTILITIES
    // ====================================================================

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        JsonObject err = new JsonObject();
        err.addProperty("success", false);
        err.addProperty("error", message);
        sendJsonResponse(exchange, statusCode, err.toString());
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                params.put(
                    URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(pair[1], StandardCharsets.UTF_8)
                );
            } else if (pair.length == 1) {
                params.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), "");
            }
        }
        return params;
    }
}
