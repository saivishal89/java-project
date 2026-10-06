package com.studentmanagement.dao;

import com.studentmanagement.config.DatabaseConnection;
import com.studentmanagement.model.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * StudentDAO.java  (DAO = Data Access Object)
 * =============================================
 * PURPOSE: Contains ALL the database operations for the students table.
 *
 * WHY THIS CLASS EXISTS:
 *   This class is the ONLY place where SQL queries are written and executed.
 *   It separates database logic from business logic (StudentService) and
 *   UI logic (Main.java).
 *
 *   If you ever change the database (e.g., table structure), you only
 *   need to modify THIS class. The rest of the application stays the same.
 *
 * JDBC CONCEPTS DEMONSTRATED:
 *   1. PreparedStatement  — prevents SQL injection, used for ALL queries
 *   2. ResultSet          — holds the rows returned by SELECT queries
 *   3. executeQuery()     — used for SELECT (returns ResultSet)
 *   4. executeUpdate()    — used for INSERT, UPDATE, DELETE (returns row count)
 *   5. try-with-resources — auto-closes Connection, PreparedStatement, ResultSet
 *   6. SQLException       — caught and handled for every database operation
 *
 * DATA FLOW:
 *   StudentService  →  StudentDAO  →  JDBC  →  MySQL
 */
public class StudentDAO {

    // ===== SQL QUERIES =====
    // We define SQL strings as constants for clarity and maintainability.
    // The '?' placeholders are filled in by PreparedStatement to prevent SQL injection.

    private static final String INSERT_STUDENT =
        "INSERT INTO students (name, email, phone, age, gender, course, department, semester, address) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_ALL =
        "SELECT * FROM students ORDER BY id";

    private static final String SELECT_BY_ID =
        "SELECT * FROM students WHERE id = ?";

    private static final String UPDATE_STUDENT =
        "UPDATE students SET name=?, email=?, phone=?, age=?, gender=?, " +
        "course=?, department=?, semester=?, address=? WHERE id=?";

    private static final String DELETE_STUDENT =
        "DELETE FROM students WHERE id = ?";

    private static final String CHECK_EMAIL =
        "SELECT COUNT(*) FROM students WHERE email = ?";

    private static final String CHECK_EMAIL_EXCLUDE_ID =
        "SELECT COUNT(*) FROM students WHERE email = ? AND id != ?";

    private static final String SEARCH_BY_NAME =
        "SELECT * FROM students WHERE name LIKE ? ORDER BY id";

    private static final String SEARCH_BY_EMAIL =
        "SELECT * FROM students WHERE email LIKE ? ORDER BY id";

    private static final String SEARCH_BY_COURSE =
        "SELECT * FROM students WHERE course LIKE ? ORDER BY id";

    private static final String SEARCH_BY_DEPARTMENT =
        "SELECT * FROM students WHERE department LIKE ? ORDER BY id";

    // ====================================================================
    //                         CREATE OPERATION
    // ====================================================================

    /**
     * Adds a new student to the database.
     *
     * HOW IT WORKS:
     *   1. Get a Connection from DatabaseConnection
     *   2. Create a PreparedStatement with the INSERT SQL
     *   3. Set each '?' placeholder with the student's data
     *   4. Execute the statement with executeUpdate()
     *   5. Return true if 1 row was inserted
     *
     * @param student the Student object to insert
     * @return true if the student was added successfully
     */
    public boolean addStudent(Student student) {
        /*
         * try-with-resources:
         *   The Connection and PreparedStatement are declared inside the try().
         *   Java automatically calls .close() on them when the block ends,
         *   even if an exception occurs. This prevents resource leaks.
         */
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_STUDENT)) {

            /*
             * PreparedStatement.setXxx(index, value):
             *   - index starts at 1 (not 0!)
             *   - Each call fills in one '?' placeholder in the SQL
             *   - This is SAFE from SQL injection because the driver
             *     escapes special characters automatically
             */
            pstmt.setString(1, student.getName());
            pstmt.setString(2, student.getEmail());
            pstmt.setString(3, student.getPhone());
            pstmt.setInt(4, student.getAge());
            pstmt.setString(5, student.getGender());
            pstmt.setString(6, student.getCourse());
            pstmt.setString(7, student.getDepartment());
            pstmt.setInt(8, student.getSemester());
            pstmt.setString(9, student.getAddress());

            /*
             * executeUpdate():
             *   Used for INSERT, UPDATE, DELETE statements.
             *   Returns the number of rows affected.
             *   For a successful INSERT, this should be 1.
             */
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to add student: " + e.getMessage());
            return false;
        }
    }

    // ====================================================================
    //                         READ OPERATIONS
    // ====================================================================

    /**
     * Retrieves ALL students from the database.
     *
     * HOW IT WORKS:
     *   1. Execute a SELECT * query
     *   2. Iterate through the ResultSet
     *   3. For each row, create a Student object using extractStudentFromResultSet()
     *   4. Add each Student to a List
     *   5. Return the List
     *
     * @return List of all Student objects
     */
    public List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();

        /*
         * try-with-resources with three resources:
         *   Connection → PreparedStatement → ResultSet
         *   All three are automatically closed when the block ends.
         */
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = pstmt.executeQuery()) {

            /*
             * ResultSet.next():
             *   Moves the cursor to the next row.
             *   Returns true if there IS a next row, false if we've reached the end.
             *   This is how we iterate through query results.
             */
            while (rs.next()) {
                students.add(extractStudentFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to fetch students: " + e.getMessage());
        }

        return students;
    }

    /**
     * Retrieves a single student by their ID.
     *
     * @param id the student ID to look up
     * @return the Student object, or null if not found
     */
    public Student getStudentById(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_ID)) {

            // Set the '?' placeholder to the given ID
            pstmt.setInt(1, id);

            /*
             * executeQuery():
             *   Used for SELECT statements.
             *   Returns a ResultSet containing the matching rows.
             */
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractStudentFromResultSet(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to fetch student by ID: " + e.getMessage());
        }

        return null; // Student not found
    }

    /**
     * Searches students by a specific field.
     *
     * Uses SQL LIKE with wildcards for partial matching:
     *   LIKE '%keyword%' matches any record containing the keyword.
     *
     * @param searchField which field to search ("name", "email", "course", "department")
     * @param keyword     the search term
     * @return List of matching students
     */
    public List<Student> searchStudents(String searchField, String keyword) {
        // Pick the right SQL query based on the search field
        String query;
        switch (searchField.toLowerCase()) {
            case "name":
                query = SEARCH_BY_NAME;
                break;
            case "email":
                query = SEARCH_BY_EMAIL;
                break;
            case "course":
                query = SEARCH_BY_COURSE;
                break;
            case "department":
                query = SEARCH_BY_DEPARTMENT;
                break;
            default:
                System.err.println("[ERROR] Invalid search field: " + searchField);
                return new ArrayList<>();
        }

        List<Student> students = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            // '%keyword%' — matches the keyword anywhere in the field
            pstmt.setString(1, "%" + keyword + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    students.add(extractStudentFromResultSet(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] Search failed: " + e.getMessage());
        }

        return students;
    }

    // ====================================================================
    //                         UPDATE OPERATION
    // ====================================================================

    /**
     * Updates an existing student record in the database.
     *
     * HOW IT WORKS:
     *   1. Prepare an UPDATE statement with all fields
     *   2. Set each parameter from the Student object
     *   3. The WHERE clause uses the student's ID
     *   4. executeUpdate() returns the number of rows updated
     *
     * @param student the Student object with updated data (must have valid id)
     * @return true if the student was updated successfully
     */
    public boolean updateStudent(Student student) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_STUDENT)) {

            pstmt.setString(1, student.getName());
            pstmt.setString(2, student.getEmail());
            pstmt.setString(3, student.getPhone());
            pstmt.setInt(4, student.getAge());
            pstmt.setString(5, student.getGender());
            pstmt.setString(6, student.getCourse());
            pstmt.setString(7, student.getDepartment());
            pstmt.setInt(8, student.getSemester());
            pstmt.setString(9, student.getAddress());
            pstmt.setInt(10, student.getId());  // WHERE id = ?

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to update student: " + e.getMessage());
            return false;
        }
    }

    // ====================================================================
    //                         DELETE OPERATION
    // ====================================================================

    /**
     * Deletes a student from the database by their ID.
     *
     * @param id the ID of the student to delete
     * @return true if the student was deleted successfully
     */
    public boolean deleteStudent(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(DELETE_STUDENT)) {

            pstmt.setInt(1, id);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to delete student: " + e.getMessage());
            return false;
        }
    }

    // ====================================================================
    //                       UTILITY METHODS
    // ====================================================================

    /**
     * Checks if an email already exists in the database.
     * Used to prevent duplicate email entries.
     *
     * @param email the email to check
     * @return true if the email already exists
     */
    public boolean emailExists(String email) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(CHECK_EMAIL)) {

            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // COUNT(*) returns the number of rows with this email
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to check email: " + e.getMessage());
        }

        return false;
    }

    /**
     * Checks if an email exists for a student OTHER than the given ID.
     * Used during update to allow a student to keep their own email
     * but prevent them from taking someone else's email.
     *
     * @param email the email to check
     * @param excludeId the student ID to exclude from the check
     * @return true if the email belongs to another student
     */
    public boolean emailExistsForOtherStudent(String email, int excludeId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(CHECK_EMAIL_EXCLUDE_ID)) {

            pstmt.setString(1, email);
            pstmt.setInt(2, excludeId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to check email: " + e.getMessage());
        }

        return false;
    }

    /**
     * Helper method: Extracts a Student object from the current ResultSet row.
     *
     * ResultSet.getXxx("column_name"):
     *   - getString("name")  → returns the value of the 'name' column as a String
     *   - getInt("age")      → returns the value of the 'age' column as an int
     *   - getTimestamp(...)   → returns a Timestamp value
     *
     * This avoids repeating the same extraction code in every method.
     *
     * @param rs the ResultSet positioned at the current row
     * @return a Student object populated from the current row
     * @throws SQLException if a column cannot be read
     */
    private Student extractStudentFromResultSet(ResultSet rs) throws SQLException {
        Student student = new Student();
        student.setId(rs.getInt("id"));
        student.setName(rs.getString("name"));
        student.setEmail(rs.getString("email"));
        student.setPhone(rs.getString("phone"));
        student.setAge(rs.getInt("age"));
        student.setGender(rs.getString("gender"));
        student.setCourse(rs.getString("course"));
        student.setDepartment(rs.getString("department"));
        student.setSemester(rs.getInt("semester"));
        student.setAddress(rs.getString("address"));
        student.setCreatedAt(rs.getTimestamp("created_at"));
        return student;
    }
}
