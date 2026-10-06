package com.studentmanagement.service;

import com.studentmanagement.dao.StudentDAO;
import com.studentmanagement.model.Student;
import com.studentmanagement.util.InputValidator;

import java.util.List;
import java.util.Scanner;

/**
 * StudentService.java
 * =====================
 * PURPOSE: The "middle layer" between the UI (Main.java) and the database (StudentDAO).
 *
 * WHY THIS CLASS EXISTS:
 *   Main.java handles the MENU and user interaction.
 *   StudentDAO handles the DATABASE operations.
 *   StudentService sits in between and handles:
 *     - Input collection from the user (using Scanner)
 *     - Input validation (using InputValidator)
 *     - Calling the DAO to perform database operations
 *     - Displaying results in a formatted way
 *
 * DATA FLOW:
 *   User → Main.java → StudentService → StudentDAO → MySQL
 *                                      ↑
 *                              InputValidator
 *
 * This separation means:
 *   - Main.java stays clean (just a menu)
 *   - StudentDAO stays focused (just SQL)
 *   - StudentService handles the "business logic"
 */
public class StudentService {

    // The DAO instance — this is our gateway to the database
    private final StudentDAO studentDAO;

    // Scanner for reading user input from the console
    private final Scanner scanner;

    /**
     * Constructor — receives a Scanner so we use the same one throughout the app.
     */
    public StudentService(Scanner scanner) {
        this.studentDAO = new StudentDAO();
        this.scanner = scanner;
    }

    // ====================================================================
    //                     1. ADD STUDENT (CREATE)
    // ====================================================================

    /**
     * Collects student information from the user, validates it,
     * and saves it to the database.
     */
    public void addStudent() {
        System.out.println("\n==================================================");
        System.out.println("              ADD NEW STUDENT");
        System.out.println("==================================================");

        // Collect and validate each field
        String name = getValidatedName();
        String email = getValidatedEmail();
        String phone = getValidatedPhone();
        int age = getValidatedAge();
        String gender = getValidatedGender();
        String course = getValidatedStringInput("Enter Course: ");
        String department = getValidatedStringInput("Enter Department: ");
        int semester = getValidatedSemester();
        String address = getValidatedStringInput("Enter Address: ");

        // Check if email already exists in the database
        if (studentDAO.emailExists(email)) {
            System.out.println("\n[!] A student with this email already exists. Student not added.");
            return;
        }

        // Create a Student object with the validated data
        Student student = new Student(name, email, phone, age, gender, course, department, semester, address);

        // Call the DAO to insert into MySQL
        if (studentDAO.addStudent(student)) {
            System.out.println("\n[✓] Student added successfully!");
        } else {
            System.out.println("\n[✗] Failed to add student. Please try again.");
        }
    }

    // ====================================================================
    //                   2. VIEW ALL STUDENTS (READ)
    // ====================================================================

    /**
     * Fetches all students from the database and displays them
     * in a formatted table.
     */
    public void viewAllStudents() {
        System.out.println("\n==================================================");
        System.out.println("              ALL STUDENTS");
        System.out.println("==================================================");

        List<Student> students = studentDAO.getAllStudents();

        if (students.isEmpty()) {
            System.out.println("No students found in the database.");
            return;
        }

        displayStudentTable(students);
        System.out.println("\nTotal students: " + students.size());
    }

    // ====================================================================
    //                  3. SEARCH STUDENTS (READ)
    // ====================================================================

    /**
     * Allows the user to search for students by different fields.
     */
    public void searchStudent() {
        System.out.println("\n==================================================");
        System.out.println("              SEARCH STUDENT");
        System.out.println("==================================================");
        System.out.println("Search by:");
        System.out.println("  1. Name");
        System.out.println("  2. Email");
        System.out.println("  3. Course");
        System.out.println("  4. Department");
        System.out.println("==================================================");
        System.out.print("Enter your choice: ");

        int choice = InputValidator.parseIntSafe(scanner.nextLine());
        String searchField;

        switch (choice) {
            case 1: searchField = "name";       break;
            case 2: searchField = "email";      break;
            case 3: searchField = "course";     break;
            case 4: searchField = "department"; break;
            default:
                System.out.println("Invalid choice.");
                return;
        }

        System.out.print("Enter search keyword: ");
        String keyword = scanner.nextLine().trim();

        if (keyword.isEmpty()) {
            System.out.println("Search keyword cannot be empty.");
            return;
        }

        List<Student> results = studentDAO.searchStudents(searchField, keyword);

        if (results.isEmpty()) {
            System.out.println("\nNo students found matching '" + keyword + "' in " + searchField + ".");
        } else {
            System.out.println("\nSearch Results (" + results.size() + " found):");
            displayStudentTable(results);
        }
    }

    // ====================================================================
    //                  4. VIEW STUDENT BY ID (READ)
    // ====================================================================

    /**
     * Displays detailed information for a single student.
     */
    public void viewStudentById() {
        System.out.println("\n==================================================");
        System.out.println("              VIEW STUDENT BY ID");
        System.out.println("==================================================");
        System.out.print("Enter Student ID: ");

        int id = InputValidator.parseIntSafe(scanner.nextLine());
        if (id <= 0) {
            System.out.println("Invalid ID. Please enter a positive number.");
            return;
        }

        Student student = studentDAO.getStudentById(id);

        if (student == null) {
            System.out.println("\nStudent with ID " + id + " not found.");
        } else {
            displayStudentDetails(student);
        }
    }

    // ====================================================================
    //                   5. UPDATE STUDENT (UPDATE)
    // ====================================================================

    /**
     * Allows the user to update a student's information.
     * First fetches the current data, then asks what to update.
     */
    public void updateStudent() {
        System.out.println("\n==================================================");
        System.out.println("              UPDATE STUDENT");
        System.out.println("==================================================");
        System.out.print("Enter Student ID to update: ");

        int id = InputValidator.parseIntSafe(scanner.nextLine());
        if (id <= 0) {
            System.out.println("Invalid ID. Please enter a positive number.");
            return;
        }

        // Fetch the current student data
        Student student = studentDAO.getStudentById(id);
        if (student == null) {
            System.out.println("\nStudent with ID " + id + " not found.");
            return;
        }

        // Show current information
        System.out.println("\nCurrent Student Information:");
        displayStudentDetails(student);

        // Show update menu
        System.out.println("\n--------------------------------------------------");
        System.out.println("What would you like to update?");
        System.out.println("--------------------------------------------------");
        System.out.println("  1. Name");
        System.out.println("  2. Email");
        System.out.println("  3. Phone");
        System.out.println("  4. Age");
        System.out.println("  5. Gender");
        System.out.println("  6. Course");
        System.out.println("  7. Department");
        System.out.println("  8. Semester");
        System.out.println("  9. Address");
        System.out.println("  10. Update ALL Fields");
        System.out.println("  0. Cancel");
        System.out.println("--------------------------------------------------");
        System.out.print("Enter your choice: ");

        int choice = InputValidator.parseIntSafe(scanner.nextLine());

        switch (choice) {
            case 0:
                System.out.println("Update cancelled.");
                return;
            case 1:
                student.setName(getValidatedName());
                break;
            case 2:
                String newEmail = getValidatedEmail();
                if (studentDAO.emailExistsForOtherStudent(newEmail, id)) {
                    System.out.println("[!] This email is already used by another student.");
                    return;
                }
                student.setEmail(newEmail);
                break;
            case 3:
                student.setPhone(getValidatedPhone());
                break;
            case 4:
                student.setAge(getValidatedAge());
                break;
            case 5:
                student.setGender(getValidatedGender());
                break;
            case 6:
                student.setCourse(getValidatedStringInput("Enter new Course: "));
                break;
            case 7:
                student.setDepartment(getValidatedStringInput("Enter new Department: "));
                break;
            case 8:
                student.setSemester(getValidatedSemester());
                break;
            case 9:
                student.setAddress(getValidatedStringInput("Enter new Address: "));
                break;
            case 10:
                student.setName(getValidatedName());
                String allEmail = getValidatedEmail();
                if (studentDAO.emailExistsForOtherStudent(allEmail, id)) {
                    System.out.println("[!] This email is already used by another student.");
                    return;
                }
                student.setEmail(allEmail);
                student.setPhone(getValidatedPhone());
                student.setAge(getValidatedAge());
                student.setGender(getValidatedGender());
                student.setCourse(getValidatedStringInput("Enter Course: "));
                student.setDepartment(getValidatedStringInput("Enter Department: "));
                student.setSemester(getValidatedSemester());
                student.setAddress(getValidatedStringInput("Enter Address: "));
                break;
            default:
                System.out.println("Invalid choice.");
                return;
        }

        // Save the updated student to the database
        if (studentDAO.updateStudent(student)) {
            System.out.println("\n[✓] Student updated successfully!");
        } else {
            System.out.println("\n[✗] Failed to update student.");
        }
    }

    // ====================================================================
    //                   6. DELETE STUDENT (DELETE)
    // ====================================================================

    /**
     * Deletes a student after confirmation.
     */
    public void deleteStudent() {
        System.out.println("\n==================================================");
        System.out.println("              DELETE STUDENT");
        System.out.println("==================================================");
        System.out.print("Enter Student ID to delete: ");

        int id = InputValidator.parseIntSafe(scanner.nextLine());
        if (id <= 0) {
            System.out.println("Invalid ID. Please enter a positive number.");
            return;
        }

        // Fetch the student to show details before deletion
        Student student = studentDAO.getStudentById(id);
        if (student == null) {
            System.out.println("\nStudent with ID " + id + " not found.");
            return;
        }

        // Show the student info
        displayStudentDetails(student);

        // Ask for confirmation
        System.out.print("\nAre you sure you want to delete this student? (Y/N): ");
        String confirmation = scanner.nextLine().trim().toUpperCase();

        if (confirmation.equals("Y") || confirmation.equals("YES")) {
            if (studentDAO.deleteStudent(id)) {
                System.out.println("\n[✓] Student deleted successfully!");
            } else {
                System.out.println("\n[✗] Failed to delete student.");
            }
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    // ====================================================================
    //                    DISPLAY HELPER METHODS
    // ====================================================================

    /**
     * Displays a list of students in a clean table format.
     */
    private void displayStudentTable(List<Student> students) {
        String line = "+" + "-".repeat(6) + "+" + "-".repeat(22) + "+"
                    + "-".repeat(27) + "+" + "-".repeat(22) + "+"
                    + "-".repeat(14) + "+" + "-".repeat(6) + "+";

        System.out.println(line);
        System.out.printf("| %-4s | %-20s | %-25s | %-20s | %-12s | %-4s |%n",
                "ID", "NAME", "EMAIL", "COURSE", "DEPARTMENT", "SEM");
        System.out.println(line);

        for (Student s : students) {
            System.out.printf("| %-4d | %-20s | %-25s | %-20s | %-12s | %-4d |%n",
                    s.getId(),
                    truncate(s.getName(), 20),
                    truncate(s.getEmail(), 25),
                    truncate(s.getCourse(), 20),
                    truncate(s.getDepartment(), 12),
                    s.getSemester());
        }

        System.out.println(line);
    }

    /**
     * Displays detailed information for a single student.
     */
    private void displayStudentDetails(Student student) {
        System.out.println("\n--------------------------------------------------");
        System.out.println("  STUDENT DETAILS");
        System.out.println("--------------------------------------------------");
        System.out.printf("  ID         : %d%n", student.getId());
        System.out.printf("  Name       : %s%n", student.getName());
        System.out.printf("  Email      : %s%n", student.getEmail());
        System.out.printf("  Phone      : %s%n", student.getPhone());
        System.out.printf("  Age        : %d%n", student.getAge());
        System.out.printf("  Gender     : %s%n", student.getGender());
        System.out.printf("  Course     : %s%n", student.getCourse());
        System.out.printf("  Department : %s%n", student.getDepartment());
        System.out.printf("  Semester   : %d%n", student.getSemester());
        System.out.printf("  Address    : %s%n", student.getAddress());
        if (student.getCreatedAt() != null) {
            System.out.printf("  Created At : %s%n", student.getCreatedAt());
        }
        System.out.println("--------------------------------------------------");
    }

    /**
     * Truncates a string if it exceeds the given length, appending "..".
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 2) + "..";
    }

    // ====================================================================
    //                 INPUT VALIDATION HELPERS
    // ====================================================================

    /**
     * Asks for a name and re-prompts until valid input is given.
     */
    private String getValidatedName() {
        while (true) {
            System.out.print("Enter Student Name: ");
            String name = scanner.nextLine().trim();
            if (InputValidator.isValidName(name)) {
                return name;
            }
            System.out.println("  [!] Invalid name. Use only letters, spaces, dots, and hyphens (2-100 chars).");
        }
    }

    /**
     * Asks for an email and re-prompts until valid input is given.
     */
    private String getValidatedEmail() {
        while (true) {
            System.out.print("Enter Email: ");
            String email = scanner.nextLine().trim();
            if (InputValidator.isValidEmail(email)) {
                return email;
            }
            System.out.println("  [!] Invalid email format. Example: name@example.com");
        }
    }

    /**
     * Asks for a phone number and re-prompts until valid input is given.
     */
    private String getValidatedPhone() {
        while (true) {
            System.out.print("Enter Phone: ");
            String phone = scanner.nextLine().trim();
            if (InputValidator.isValidPhone(phone)) {
                return phone;
            }
            System.out.println("  [!] Invalid phone. Enter 10-15 digits (optional + prefix).");
        }
    }

    /**
     * Asks for age and re-prompts until valid input is given.
     */
    private int getValidatedAge() {
        while (true) {
            System.out.print("Enter Age: ");
            int age = InputValidator.parseIntSafe(scanner.nextLine());
            if (InputValidator.isValidAge(age)) {
                return age;
            }
            System.out.println("  [!] Invalid age. Must be between 15 and 60.");
        }
    }

    /**
     * Asks for gender and re-prompts until valid input is given.
     */
    private String getValidatedGender() {
        while (true) {
            System.out.print("Enter Gender (Male/Female/Other): ");
            String gender = scanner.nextLine().trim();
            if (InputValidator.isValidGender(gender)) {
                // Capitalize first letter for consistency
                return gender.substring(0, 1).toUpperCase() + gender.substring(1).toLowerCase();
            }
            System.out.println("  [!] Invalid gender. Enter Male, Female, or Other.");
        }
    }

    /**
     * Asks for semester and re-prompts until valid input is given.
     */
    private int getValidatedSemester() {
        while (true) {
            System.out.print("Enter Semester (1-8): ");
            int semester = InputValidator.parseIntSafe(scanner.nextLine());
            if (InputValidator.isValidSemester(semester)) {
                return semester;
            }
            System.out.println("  [!] Invalid semester. Must be between 1 and 8.");
        }
    }

    /**
     * Asks for a non-empty string input with the given prompt.
     */
    private String getValidatedStringInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (InputValidator.isNotEmpty(input)) {
                return input;
            }
            System.out.println("  [!] This field cannot be empty. Please try again.");
        }
    }
}
