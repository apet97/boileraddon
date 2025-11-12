package com.example.rules.validation;

import java.util.regex.Pattern;

/**
 * Input validation utility for defense-in-depth security.
 * Validates user inputs against expected patterns and constraints.
 *
 * <p>This layer provides additional protection beyond HTML escaping by
 * rejecting malformed inputs before processing.</p>
 */
public class InputValidator {

    // MongoDB ObjectId format (24 hex characters)
    private static final Pattern WORKSPACE_ID_PATTERN = Pattern.compile("^[a-fA-F0-9]{24}$");
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-fA-F0-9]{24}$");
    private static final Pattern PROJECT_ID_PATTERN = Pattern.compile("^[a-fA-F0-9]{24}$");
    private static final Pattern TAG_ID_PATTERN = Pattern.compile("^[a-fA-F0-9]{24}$");

    // Safe string pattern: alphanumeric, spaces, and common punctuation
    // Excludes script injection characters like < > " ' & etc.
    private static final Pattern SAFE_STRING_PATTERN = Pattern.compile("^[\\w\\s.,-]{1,500}$");

    // Rule name: more restrictive (no special chars except dash, underscore, space)
    private static final Pattern RULE_NAME_PATTERN = Pattern.compile("^[\\w\\s-]{1,200}$");

    // Description: allows more characters but limited length
    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("^[\\w\\s.,!?()\\[\\]:;\"'-]{0,2000}$");

    // Max lengths for various inputs
    private static final int MAX_RULE_NAME_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final int MAX_TAG_NAME_LENGTH = 100;
    private static final int MAX_PROJECT_NAME_LENGTH = 200;

    /**
     * Validates a Clockify workspace ID.
     *
     * @param workspaceId The workspace ID to validate
     * @throws ValidationException if the workspace ID is invalid
     */
    public static void validateWorkspaceId(String workspaceId) throws ValidationException {
        if (workspaceId == null || workspaceId.isBlank()) {
            throw new ValidationException("Workspace ID is required");
        }
        if (!WORKSPACE_ID_PATTERN.matcher(workspaceId).matches()) {
            throw new ValidationException("Invalid workspace ID format (expected 24-character hex string)");
        }
    }

    /**
     * Validates a Clockify user ID.
     *
     * @param userId The user ID to validate
     * @throws ValidationException if the user ID is invalid
     */
    public static void validateUserId(String userId) throws ValidationException {
        if (userId == null || userId.isBlank()) {
            throw new ValidationException("User ID is required");
        }
        if (!USER_ID_PATTERN.matcher(userId).matches()) {
            throw new ValidationException("Invalid user ID format (expected 24-character hex string)");
        }
    }

    /**
     * Validates a Clockify project ID.
     *
     * @param projectId The project ID to validate
     * @throws ValidationException if the project ID is invalid
     */
    public static void validateProjectId(String projectId) throws ValidationException {
        if (projectId == null || projectId.isBlank()) {
            throw new ValidationException("Project ID is required");
        }
        if (!PROJECT_ID_PATTERN.matcher(projectId).matches()) {
            throw new ValidationException("Invalid project ID format (expected 24-character hex string)");
        }
    }

    /**
     * Validates a rule name.
     *
     * @param name The rule name to validate
     * @throws ValidationException if the rule name is invalid
     */
    public static void validateRuleName(String name) throws ValidationException {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Rule name is required");
        }
        if (name.length() > MAX_RULE_NAME_LENGTH) {
            throw new ValidationException("Rule name must be " + MAX_RULE_NAME_LENGTH + " characters or less");
        }
        if (!RULE_NAME_PATTERN.matcher(name).matches()) {
            throw new ValidationException("Rule name contains invalid characters (only letters, numbers, spaces, dashes, and underscores allowed)");
        }
    }

    /**
     * Validates a description field.
     *
     * @param description The description to validate
     * @throws ValidationException if the description is invalid
     */
    public static void validateDescription(String description) throws ValidationException {
        if (description == null) {
            return; // Description is optional
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException("Description must be " + MAX_DESCRIPTION_LENGTH + " characters or less");
        }
        if (!DESCRIPTION_PATTERN.matcher(description).matches()) {
            throw new ValidationException("Description contains invalid characters");
        }
    }

    /**
     * Validates a tag name.
     *
     * @param tagName The tag name to validate
     * @throws ValidationException if the tag name is invalid
     */
    public static void validateTagName(String tagName) throws ValidationException {
        if (tagName == null || tagName.isBlank()) {
            throw new ValidationException("Tag name is required");
        }
        if (tagName.length() > MAX_TAG_NAME_LENGTH) {
            throw new ValidationException("Tag name must be " + MAX_TAG_NAME_LENGTH + " characters or less");
        }
        if (!SAFE_STRING_PATTERN.matcher(tagName).matches()) {
            throw new ValidationException("Tag name contains invalid characters");
        }
    }

    /**
     * Validates a project name.
     *
     * @param projectName The project name to validate
     * @throws ValidationException if the project name is invalid
     */
    public static void validateProjectName(String projectName) throws ValidationException {
        if (projectName == null || projectName.isBlank()) {
            throw new ValidationException("Project name is required");
        }
        if (projectName.length() > MAX_PROJECT_NAME_LENGTH) {
            throw new ValidationException("Project name must be " + MAX_PROJECT_NAME_LENGTH + " characters or less");
        }
        if (!SAFE_STRING_PATTERN.matcher(projectName).matches()) {
            throw new ValidationException("Project name contains invalid characters");
        }
    }

    /**
     * Validates a generic safe string (alphanumeric + basic punctuation only).
     *
     * @param input The string to validate
     * @param fieldName The name of the field (for error messages)
     * @throws ValidationException if the string is invalid
     */
    public static void validateSafeString(String input, String fieldName) throws ValidationException {
        if (input == null) {
            return; // Null is acceptable for optional fields
        }
        if (input.length() > 500) {
            throw new ValidationException(fieldName + " must be 500 characters or less");
        }
        if (!SAFE_STRING_PATTERN.matcher(input).matches()) {
            throw new ValidationException(fieldName + " contains invalid characters");
        }
    }

    /**
     * Exception thrown when validation fails.
     */
    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}
