package com.studentmanagement.util;

/**
 * InputValidator.java
 * ====================
 * PURPOSE: Validates all user input before it reaches the database.
 *
 * WHY THIS CLASS EXISTS:
 *   We never trust user input. Before sending data to MySQL, we check:
 *     - Is the name valid?
 *     - Is the email in correct format?
 *     - Is the phone number numeric and proper length?
 *     - Is the age reasonable?
 *     - Is the semester within range?
 *
 *   This prevents:
 *     - Garbage data in the database
 *     - Application crashes from bad input
 *     - SQL errors from invalid values
 */
public class InputValidator {

    /**
     * Validates a student name.
     * Rules:
     *   - Cannot be null or empty
     *   - Must be 2-100 characters
     *   - Should contain only letters, spaces, and dots
     *
     * @param name the name to validate
     * @return true if valid
     */
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        String trimmed = name.trim();
        // Allow letters (including unicode), spaces, dots, and hyphens
        return trimmed.length() >= 2
            && trimmed.length() <= 100
            && trimmed.matches("[a-zA-Z\\s.'-]+");
    }

    /**
     * Validates an email address using a basic pattern.
     * Rules:
     *   - Cannot be null or empty
     *   - Must contain @ and a domain with a dot
     *   - Must be <= 150 characters
     *
     * @param email the email to validate
     * @return true if valid
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String trimmed = email.trim();
        // Basic email regex: something@something.something
        return trimmed.length() <= 150
            && trimmed.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Validates a phone number.
     * Rules:
     *   - Cannot be null or empty
     *   - Must be 10-15 digits
     *   - Must contain only digits (optionally starting with +)
     *
     * @param phone the phone number to validate
     * @return true if valid
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        String trimmed = phone.trim();
        // Allow optional + prefix, then 10-15 digits
        return trimmed.matches("^\\+?[0-9]{10,15}$");
    }

    /**
     * Validates age.
     * Rules:
     *   - Must be between 15 and 60 (reasonable for a student)
     *
     * @param age the age to validate
     * @return true if valid
     */
    public static boolean isValidAge(int age) {
        return age >= 15 && age <= 60;
    }

    /**
     * Validates semester.
     * Rules:
     *   - Must be between 1 and 8
     *
     * @param semester the semester to validate
     * @return true if valid
     */
    public static boolean isValidSemester(int semester) {
        return semester >= 1 && semester <= 8;
    }

    /**
     * Validates gender.
     * Rules:
     *   - Must be "Male", "Female", or "Other" (case-insensitive)
     *
     * @param gender the gender to validate
     * @return true if valid
     */
    public static boolean isValidGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            return false;
        }
        String lower = gender.trim().toLowerCase();
        return lower.equals("male") || lower.equals("female") || lower.equals("other");
    }

    /**
     * Checks if a string is not null and not empty.
     *
     * @param input the string to check
     * @return true if the string has content
     */
    public static boolean isNotEmpty(String input) {
        return input != null && !input.trim().isEmpty();
    }

    /**
     * Safely parses a string to an integer.
     * Returns -1 if the string is not a valid number.
     *
     * @param input the string to parse
     * @return the parsed integer, or -1 if invalid
     */
    public static int parseIntSafe(String input) {
        if (input == null || input.trim().isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
