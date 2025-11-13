package com.example.rules.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputValidatorTest {

    @Test
    void validateWorkspaceId_accepts24Hex_andRejectsInvalid() throws Exception {
        // ok: 24 hex
        assertDoesNotThrow(() -> InputValidator.validateWorkspaceId("0123456789abcdefABCDEF01"));

        // null / blank
        assertThrows(InputValidator.ValidationException.class, () -> InputValidator.validateWorkspaceId(null));
        assertThrows(InputValidator.ValidationException.class, () -> InputValidator.validateWorkspaceId(""));

        // wrong length / bad chars
        assertThrows(InputValidator.ValidationException.class, () -> InputValidator.validateWorkspaceId("1234"));
        assertThrows(InputValidator.ValidationException.class, () -> InputValidator.validateWorkspaceId("zzzzzzzzzzzzzzzzzzzzzzzz"));
    }

    @Test
    void validateUserAndProjectId_coverHappyAndSadPaths() throws Exception {
        assertDoesNotThrow(() -> InputValidator.validateUserId("abcdefabcdefabcdefabcdef"));
        assertThrows(InputValidator.ValidationException.class, () -> InputValidator.validateUserId("nothex"));

        assertDoesNotThrow(() -> InputValidator.validateProjectId("00112233445566778899AAbb"));
        assertThrows(InputValidator.ValidationException.class, () -> InputValidator.validateProjectId("short"));
    }

    @Test
    void validateRuleName_boundsAndChars() throws Exception {
        assertDoesNotThrow(() -> InputValidator.validateRuleName("My Rule_1-OK"));

        // too long
        String longName = "x".repeat(201);
        assertThrows(InputValidator.ValidationException.class, () -> InputValidator.validateRuleName(longName));

        // invalid chars
        assertThrows(InputValidator.ValidationException.class, () -> InputValidator.validateRuleName("Bad<Name>"));
    }

    @Test
    void validateDescription_optionalButChecked() throws Exception {
        // null allowed
        assertDoesNotThrow(() -> InputValidator.validateDescription(null));

        // too long
        String longDesc = "y".repeat(2001);
        assertThrows(InputValidator.ValidationException.class, () -> InputValidator.validateDescription(longDesc));

        // invalid characters like '<' should be rejected
        assertThrows(InputValidator.ValidationException.class, () -> InputValidator.validateDescription("<script>"));
    }

    @Test
    void validateTagAndProjectName_boundsAndChars() throws Exception {
        assertDoesNotThrow(() -> InputValidator.validateTagName("tag-1"));
        assertDoesNotThrow(() -> InputValidator.validateProjectName("Project 123"));

        String veryLongTag = "t".repeat(101);
        assertThrows(InputValidator.ValidationException.class, () -> InputValidator.validateTagName(veryLongTag));
        assertThrows(InputValidator.ValidationException.class, () -> InputValidator.validateTagName("bad<name>"));

        String veryLongProject = "p".repeat(201);
        assertThrows(InputValidator.ValidationException.class, () -> InputValidator.validateProjectName(veryLongProject));
        assertThrows(InputValidator.ValidationException.class, () -> InputValidator.validateProjectName("bad<name>"));
    }

    @Test
    void validateSafeString_coversNullLengthAndInvalid() throws Exception {
        assertDoesNotThrow(() -> InputValidator.validateSafeString(null, "field"));
        assertDoesNotThrow(() -> InputValidator.validateSafeString("Alpha-123, ok", "field"));

        String longStr = "z".repeat(501);
        InputValidator.ValidationException e1 = assertThrows(InputValidator.ValidationException.class,
                () -> InputValidator.validateSafeString(longStr, "field"));
        assertTrue(e1.getMessage().contains("500"));

        InputValidator.ValidationException e2 = assertThrows(InputValidator.ValidationException.class,
                () -> InputValidator.validateSafeString("<invalid>", "field"));
        assertTrue(e2.getMessage().toLowerCase().contains("invalid"));
    }
}

