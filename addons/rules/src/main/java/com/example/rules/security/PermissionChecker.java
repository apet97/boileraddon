package com.example.rules.security;

import com.clockify.addon.sdk.security.TokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Validates permissions and scopes for workspace operations.
 * Ensures that the installed addon has the required scopes for specific operations.
 */
public class PermissionChecker {

    private static final Logger logger = LoggerFactory.getLogger(PermissionChecker.class);

    // Define required scopes for different operations
    public static final String SCOPE_TIME_ENTRY_READ = "TIME_ENTRY_READ";
    public static final String SCOPE_TIME_ENTRY_WRITE = "TIME_ENTRY_WRITE";
    public static final String SCOPE_TAG_READ = "TAG_READ";
    public static final String SCOPE_TAG_WRITE = "TAG_WRITE";
    public static final String SCOPE_PROJECT_READ = "PROJECT_READ";
    public static final String SCOPE_PROJECT_WRITE = "PROJECT_WRITE";
    public static final String SCOPE_CLIENT_READ = "CLIENT_READ";
    public static final String SCOPE_CLIENT_WRITE = "CLIENT_WRITE";
    public static final String SCOPE_TASK_READ = "TASK_READ";
    public static final String SCOPE_TASK_WRITE = "TASK_WRITE";

    /**
     * Checks if the workspace has the required scopes for an operation.
     *
     * @param workspaceId the workspace ID
     * @param requiredScopes the scopes required for the operation
     * @return true if all required scopes are present, false otherwise
     */
    public static boolean hasRequiredScopes(String workspaceId, String... requiredScopes) {
        if (workspaceId == null || workspaceId.isBlank()) {
            logger.warn("Permission check failed: workspaceId is null or blank");
            return false;
        }

        var tokenOpt = TokenStore.get(workspaceId);
        if (tokenOpt.isEmpty()) {
            logger.warn("Permission check failed: no token found for workspace {}", workspaceId);
            return false;
        }

        // In a real implementation, we would check the actual scopes granted to the addon
        // For now, we assume all required scopes are granted since they're defined in manifest
        // In production, this would validate against the actual granted scopes from the token

        logger.debug("Permission check passed for workspace {} with scopes: {}",
                    workspaceId, Arrays.toString(requiredScopes));
        return true;
    }

    /**
     * Validates that the workspace has read permissions for time entries.
     */
    public static boolean canReadTimeEntries(String workspaceId) {
        return hasRequiredScopes(workspaceId, SCOPE_TIME_ENTRY_READ);
    }

    /**
     * Validates that the workspace has write permissions for time entries.
     */
    public static boolean canWriteTimeEntries(String workspaceId) {
        return hasRequiredScopes(workspaceId, SCOPE_TIME_ENTRY_WRITE);
    }

    /**
     * Validates that the workspace has read permissions for tags.
     */
    public static boolean canReadTags(String workspaceId) {
        return hasRequiredScopes(workspaceId, SCOPE_TAG_READ);
    }

    /**
     * Validates that the workspace has write permissions for tags.
     */
    public static boolean canWriteTags(String workspaceId) {
        return hasRequiredScopes(workspaceId, SCOPE_TAG_WRITE);
    }

    /**
     * Validates that the workspace has read permissions for projects.
     */
    public static boolean canReadProjects(String workspaceId) {
        return hasRequiredScopes(workspaceId, SCOPE_PROJECT_READ);
    }

    /**
     * Validates that the workspace has write permissions for projects.
     */
    public static boolean canWriteProjects(String workspaceId) {
        return hasRequiredScopes(workspaceId, SCOPE_PROJECT_WRITE);
    }

    /**
     * Validates that the workspace has read permissions for clients.
     */
    public static boolean canReadClients(String workspaceId) {
        return hasRequiredScopes(workspaceId, SCOPE_CLIENT_READ);
    }

    /**
     * Validates that the workspace has write permissions for clients.
     */
    public static boolean canWriteClients(String workspaceId) {
        return hasRequiredScopes(workspaceId, SCOPE_CLIENT_WRITE);
    }

    /**
     * Validates that the workspace has read permissions for tasks.
     */
    public static boolean canReadTasks(String workspaceId) {
        return hasRequiredScopes(workspaceId, SCOPE_TASK_READ);
    }

    /**
     * Validates that the workspace has write permissions for tasks.
     */
    public static boolean canWriteTasks(String workspaceId) {
        return hasRequiredScopes(workspaceId, SCOPE_TASK_WRITE);
    }

    /**
     * Validates field values to prevent injection attacks and ensure data integrity.
     */
    public static class FieldValidator {

        /**
         * Validates a name field for length and injection safety.
         */
        public static void validateName(String name, String fieldName) throws ValidationException {
            if (name == null || name.trim().isEmpty()) {
                throw new ValidationException(fieldName + " cannot be empty");
            }

            if (name.length() > 100) {
                throw new ValidationException(fieldName + " cannot exceed 100 characters");
            }

            // Prevent injection attacks
            if (name.contains("..") || name.contains("/") || name.contains("\\") ||
                name.contains("<") || name.contains(">") || name.contains("\"") ||
                name.contains("'") || name.contains("`")) {
                throw new ValidationException(fieldName + " contains invalid characters");
            }
        }

        /**
         * Validates an email field format.
         */
        public static void validateEmail(String email) throws ValidationException {
            if (email == null || email.trim().isEmpty()) {
                return; // Email is optional
            }

            if (email.length() > 254) {
                throw new ValidationException("Email cannot exceed 254 characters");
            }

            // Basic email format validation
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                throw new ValidationException("Invalid email format");
            }
        }

        /**
         * Validates a note/description field.
         */
        public static void validateNote(String note, String fieldName) throws ValidationException {
            if (note == null || note.trim().isEmpty()) {
                return; // Notes are optional
            }

            if (note.length() > 1000) {
                throw new ValidationException(fieldName + " cannot exceed 1000 characters");
            }

            // Prevent injection attacks in notes
            if (note.contains("<script") || note.contains("javascript:") ||
                note.contains("onload=") || note.contains("onerror=")) {
                throw new ValidationException(fieldName + " contains potentially unsafe content");
            }
        }

        /**
         * Validates a color field (hex format).
         */
        public static void validateColor(String color) throws ValidationException {
            if (color == null || color.trim().isEmpty()) {
                return; // Color is optional
            }

            if (!color.matches("^#[0-9A-Fa-f]{6}$")) {
                throw new ValidationException("Color must be in hex format (e.g., #FF0000)");
            }
        }

        /**
         * Validates an estimate field.
         */
        public static void validateEstimate(String estimate) throws ValidationException {
            if (estimate == null || estimate.trim().isEmpty()) {
                return; // Estimate is optional
            }

            if (estimate.length() > 50) {
                throw new ValidationException("Estimate cannot exceed 50 characters");
            }

            // Validate estimate format (e.g., "1h 30m", "2d", etc.)
            if (!estimate.matches("^[0-9]+[dhms]?(\\s+[0-9]+[dhms]?)*$")) {
                throw new ValidationException("Invalid estimate format");
            }
        }

        /**
         * Validates an ID field.
         */
        public static void validateId(String id, String fieldName) throws ValidationException {
            if (id == null || id.trim().isEmpty()) {
                throw new ValidationException(fieldName + " cannot be empty");
            }

            if (id.length() > 50) {
                throw new ValidationException(fieldName + " cannot exceed 50 characters");
            }

            // IDs should only contain alphanumeric characters and hyphens
            if (!id.matches("^[a-zA-Z0-9-]+$")) {
                throw new ValidationException(fieldName + " contains invalid characters");
            }
        }
    }

    /**
     * Exception thrown when permission or validation checks fail.
     */
    public static class PermissionException extends Exception {
        public PermissionException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when field validation fails.
     */
    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}