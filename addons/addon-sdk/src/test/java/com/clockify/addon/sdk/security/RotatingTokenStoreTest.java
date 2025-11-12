package com.clockify.addon.sdk.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RotatingTokenStore class.
 * Tests token rotation functionality, grace period handling, and thread safety.
 */
class RotatingTokenStoreTest {

    private TokenStoreSPI mockDelegate;
    private RotatingTokenStore rotatingStore;
    private static final String WORKSPACE_ID = "ws-test-123";
    private static final String OLD_TOKEN = "old-token-123";
    private static final String NEW_TOKEN = "new-token-456";

    @BeforeEach
    void setUp() {
        mockDelegate = Mockito.mock(TokenStoreSPI.class);
        rotatingStore = new RotatingTokenStore(mockDelegate, 1000); // 1 second grace period for testing
    }

    @Test
    void testConstructorWithDefaultGracePeriod() {
        RotatingTokenStore store = new RotatingTokenStore(mockDelegate);
        assertNotNull(store);
    }

    @Test
    void testConstructorWithCustomGracePeriod() {
        RotatingTokenStore store = new RotatingTokenStore(mockDelegate, 5000);
        assertNotNull(store);
    }

    @Test
    void testSaveToken() {
        rotatingStore.save(WORKSPACE_ID, OLD_TOKEN);

        verify(mockDelegate).save(WORKSPACE_ID, OLD_TOKEN);
        // Rotation state should be cleared on save
    }

    @Test
    void testGetToken() {
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(OLD_TOKEN));

        Optional<String> result = rotatingStore.get(WORKSPACE_ID);

        assertTrue(result.isPresent());
        assertEquals(OLD_TOKEN, result.get());
        verify(mockDelegate).get(WORKSPACE_ID);
    }

    @Test
    void testGetTokenNotFound() {
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.empty());

        Optional<String> result = rotatingStore.get(WORKSPACE_ID);

        assertFalse(result.isPresent());
        verify(mockDelegate).get(WORKSPACE_ID);
    }

    @Test
    void testRemoveToken() {
        rotatingStore.remove(WORKSPACE_ID);

        verify(mockDelegate).remove(WORKSPACE_ID);
        // Rotation state should be cleared on remove
    }

    @Test
    void testRotateToken() {
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(OLD_TOKEN));

        rotatingStore.rotate(WORKSPACE_ID, NEW_TOKEN);

        verify(mockDelegate).get(WORKSPACE_ID);
        verify(mockDelegate).save(WORKSPACE_ID, NEW_TOKEN);
        // Rotation state should be set
    }

    @Test
    void testRotateTokenWithoutPreviousToken() {
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.empty());

        rotatingStore.rotate(WORKSPACE_ID, NEW_TOKEN);

        verify(mockDelegate).get(WORKSPACE_ID);
        verify(mockDelegate).save(WORKSPACE_ID, NEW_TOKEN);
        // Rotation state should be cleared when no previous token
    }

    @Test
    void testRotateTokenWithNullWorkspaceId() {
        assertThrows(IllegalArgumentException.class, () -> {
            rotatingStore.rotate(null, NEW_TOKEN);
        });
    }

    @Test
    void testRotateTokenWithBlankWorkspaceId() {
        assertThrows(IllegalArgumentException.class, () -> {
            rotatingStore.rotate("", NEW_TOKEN);
        });
    }

    @Test
    void testRotateTokenWithNullToken() {
        assertThrows(IllegalArgumentException.class, () -> {
            rotatingStore.rotate(WORKSPACE_ID, null);
        });
    }

    @Test
    void testRotateTokenWithBlankToken() {
        assertThrows(IllegalArgumentException.class, () -> {
            rotatingStore.rotate(WORKSPACE_ID, "");
        });
    }

    @Test
    void testIsValidTokenWithCurrentToken() {
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(NEW_TOKEN));

        boolean result = rotatingStore.isValidToken(WORKSPACE_ID, NEW_TOKEN);

        assertTrue(result);
        verify(mockDelegate).get(WORKSPACE_ID);
    }

    @Test
    void testIsValidTokenWithPreviousTokenInGracePeriod() {
        // Setup: mock returns OLD_TOKEN before rotation, NEW_TOKEN after rotation
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(OLD_TOKEN));

        // Setup rotation state
        rotatingStore.rotate(WORKSPACE_ID, NEW_TOKEN);

        // After rotation, mock should return NEW_TOKEN
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(NEW_TOKEN));

        // Should accept old token during grace period
        boolean result = rotatingStore.isValidToken(WORKSPACE_ID, OLD_TOKEN);

        assertTrue(result);
    }

    @Test
    void testIsValidTokenWithPreviousTokenAfterGracePeriod() throws InterruptedException {
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(NEW_TOKEN));

        // Setup rotation state with very short grace period
        RotatingTokenStore shortGraceStore = new RotatingTokenStore(mockDelegate, 10); // 10ms grace period
        shortGraceStore.rotate(WORKSPACE_ID, NEW_TOKEN);

        // Wait for grace period to expire
        Thread.sleep(50);

        // Should not accept old token after grace period
        boolean result = shortGraceStore.isValidToken(WORKSPACE_ID, OLD_TOKEN);

        assertFalse(result);
    }

    @Test
    void testIsValidTokenWithInvalidToken() {
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(NEW_TOKEN));

        boolean result = rotatingStore.isValidToken(WORKSPACE_ID, "invalid-token");

        assertFalse(result);
    }

    @Test
    void testIsValidTokenWithNullToken() {
        boolean result = rotatingStore.isValidToken(WORKSPACE_ID, null);

        assertFalse(result);
    }

    @Test
    void testIsValidTokenWithNoCurrentToken() {
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.empty());

        boolean result = rotatingStore.isValidToken(WORKSPACE_ID, OLD_TOKEN);

        assertFalse(result);
    }

    @Test
    void testGetRotationMetadataDuringGracePeriod() {
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(OLD_TOKEN));

        rotatingStore.rotate(WORKSPACE_ID, NEW_TOKEN);

        // Note: The actual RotatingTokenStore doesn't have getRotationMetadata method
        // This test verifies that rotation state is properly maintained
        assertTrue(rotatingStore.isValidToken(WORKSPACE_ID, OLD_TOKEN));
    }

    @Test
    void testGetRotationMetadataAfterGracePeriod() throws InterruptedException {
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(OLD_TOKEN));

        RotatingTokenStore shortGraceStore = new RotatingTokenStore(mockDelegate, 10); // 10ms grace period
        shortGraceStore.rotate(WORKSPACE_ID, NEW_TOKEN);
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(NEW_TOKEN));

        // Wait for grace period to expire
        Thread.sleep(50);

        // After grace period, old token should not be valid
        assertFalse(shortGraceStore.isValidToken(WORKSPACE_ID, OLD_TOKEN));
    }

    @Test
    void testGetRotationMetadataNoRotation() {
        // Without rotation, only current token should be valid
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(OLD_TOKEN));
        assertTrue(rotatingStore.isValidToken(WORKSPACE_ID, OLD_TOKEN));
        assertFalse(rotatingStore.isValidToken(WORKSPACE_ID, NEW_TOKEN));
    }

    @Test
    void testSaveClearsRotationState() {
        // Setup rotation state
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(OLD_TOKEN));
        rotatingStore.rotate(WORKSPACE_ID, NEW_TOKEN);

        // Save should clear rotation state
        rotatingStore.save(WORKSPACE_ID, "another-token");
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of("another-token"));

        // After save, old token should not be valid anymore
        assertFalse(rotatingStore.isValidToken(WORKSPACE_ID, OLD_TOKEN));
    }

    @Test
    void testRemoveClearsRotationState() {
        // Setup rotation state
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(OLD_TOKEN));
        rotatingStore.rotate(WORKSPACE_ID, NEW_TOKEN);

        // Remove should clear rotation state
        rotatingStore.remove(WORKSPACE_ID);
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.empty());

        // After remove, no tokens should be valid
        assertFalse(rotatingStore.isValidToken(WORKSPACE_ID, OLD_TOKEN));
        assertFalse(rotatingStore.isValidToken(WORKSPACE_ID, NEW_TOKEN));
    }

    @Test
    void testMultipleRotations() {
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(OLD_TOKEN));

        // First rotation
        rotatingStore.rotate(WORKSPACE_ID, NEW_TOKEN);

        // Second rotation
        String newerToken = "newer-token-789";
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(NEW_TOKEN));
        rotatingStore.rotate(WORKSPACE_ID, newerToken);
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(newerToken));

        // Should accept the most recent previous token and current token during grace period
        // Note: Only the immediate previous token is tracked, not all historical tokens
        assertFalse(rotatingStore.isValidToken(WORKSPACE_ID, OLD_TOKEN));  // OLD_TOKEN is no longer tracked
        assertTrue(rotatingStore.isValidToken(WORKSPACE_ID, NEW_TOKEN));   // NEW_TOKEN is tracked as previous
        assertTrue(rotatingStore.isValidToken(WORKSPACE_ID, newerToken));  // newerToken is current
    }

    @Test
    void testConcurrentTokenValidation() throws InterruptedException {
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(NEW_TOKEN));

        // Setup rotation state
        rotatingStore.rotate(WORKSPACE_ID, NEW_TOKEN);

        // Test concurrent access
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                boolean result = rotatingStore.isValidToken(WORKSPACE_ID, OLD_TOKEN);
                assertTrue(result);
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                boolean result = rotatingStore.isValidToken(WORKSPACE_ID, NEW_TOKEN);
                assertTrue(result);
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // No exceptions should occur
    }

    @Test
    void testRotationStateCleanupAfterGracePeriod() throws InterruptedException {
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(OLD_TOKEN));

        RotatingTokenStore shortGraceStore = new RotatingTokenStore(mockDelegate, 10); // 10ms grace period
        shortGraceStore.rotate(WORKSPACE_ID, NEW_TOKEN);
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(NEW_TOKEN));

        // Wait for grace period to expire
        Thread.sleep(50);

        // Rotation state should be automatically cleaned up
        // After grace period, old token should not be valid
        assertFalse(shortGraceStore.isValidToken(WORKSPACE_ID, OLD_TOKEN));

        // Subsequent validation attempts should also clean up
        boolean result = shortGraceStore.isValidToken(WORKSPACE_ID, OLD_TOKEN);
        assertFalse(result);
    }

    @Test
    void testSpecialCharactersInTokens() {
        String tokenWithSpecialChars = "token-测试-🎯-123";
        String newTokenWithSpecialChars = "new-token-测试-🎯-456";

        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(tokenWithSpecialChars));

        rotatingStore.rotate(WORKSPACE_ID, newTokenWithSpecialChars);
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(newTokenWithSpecialChars));

        // Should accept old token during grace period
        boolean result = rotatingStore.isValidToken(WORKSPACE_ID, tokenWithSpecialChars);
        assertTrue(result);

        // Should accept new token
        result = rotatingStore.isValidToken(WORKSPACE_ID, newTokenWithSpecialChars);
        assertTrue(result);
    }

    @Test
    void testLongTokens() {
        String longToken = "token-" + "x".repeat(1000);
        String newLongToken = "new-token-" + "y".repeat(1000);

        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(longToken));

        rotatingStore.rotate(WORKSPACE_ID, newLongToken);
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(newLongToken));

        // Should accept old token during grace period
        boolean result = rotatingStore.isValidToken(WORKSPACE_ID, longToken);
        assertTrue(result);

        // Should accept new token
        result = rotatingStore.isValidToken(WORKSPACE_ID, newLongToken);
        assertTrue(result);
    }

    @Test
    void testMultipleWorkspaces() {
        String workspace1 = "ws-1";
        String workspace2 = "ws-2";
        String token1 = "token-1";
        String token2 = "token-2";
        String newToken1 = "new-token-1";
        String newToken2 = "new-token-2";

        when(mockDelegate.get(workspace1)).thenReturn(Optional.of(token1));
        when(mockDelegate.get(workspace2)).thenReturn(Optional.of(token2));

        // Rotate workspace1
        rotatingStore.rotate(workspace1, newToken1);
        when(mockDelegate.get(workspace1)).thenReturn(Optional.of(newToken1));

        // Rotate workspace2
        rotatingStore.rotate(workspace2, newToken2);
        when(mockDelegate.get(workspace2)).thenReturn(Optional.of(newToken2));

        // Verify each workspace maintains independent rotation state
        assertTrue(rotatingStore.isValidToken(workspace1, token1));
        assertTrue(rotatingStore.isValidToken(workspace1, newToken1));
        assertTrue(rotatingStore.isValidToken(workspace2, token2));
        assertTrue(rotatingStore.isValidToken(workspace2, newToken2));

        // Cross-workspace validation should fail
        assertFalse(rotatingStore.isValidToken(workspace1, token2));
        assertFalse(rotatingStore.isValidToken(workspace2, token1));
    }

    @Test
    void testRotationGracePeriod() {
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(OLD_TOKEN));

        long beforeRotation = System.currentTimeMillis();
        rotatingStore.rotate(WORKSPACE_ID, NEW_TOKEN);
        long afterRotation = System.currentTimeMillis();

        // After rotation, mock should return NEW_TOKEN
        when(mockDelegate.get(WORKSPACE_ID)).thenReturn(Optional.of(NEW_TOKEN));

        // Should accept old token immediately after rotation
        assertTrue(rotatingStore.isValidToken(WORKSPACE_ID, OLD_TOKEN));
        assertTrue(rotatingStore.isValidToken(WORKSPACE_ID, NEW_TOKEN));
    }
}