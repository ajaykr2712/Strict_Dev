import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Instant;
import java.util.Optional;

/**
 * Unit tests for JWTRefreshTokenRotationExample
 * Tests token rotation, session management, and replay attack detection
 */
@RunWith(MockitoJUnitRunner.class)
public class JWTRefreshTokenRotationTest {

    private JWTRefreshTokenRotationExample.AuthService authService;

    @Before
    public void setUp() {
        authService = new JWTRefreshTokenRotationExample.AuthService();
    }

    @Test
    public void testLogin_ReturnsValidTokenPair() {
        // Arrange
        String username = "testuser";

        // Act
        JWTRefreshTokenRotationExample.AuthService.TokenPair result = authService.login(username);

        // Assert
        assertNotNull("Token pair should not be null", result);
        assertNotNull("Access token should not be null", result.access());
        assertNotNull("Refresh token should not be null", result.refresh());
        assertTrue("Access token should contain username", 
                   result.access().jwt().contains(username));
    }

    @Test
    public void testRefresh_WithValidToken_ReturnsNewPair() {
        // Arrange
        String username = "testuser";
        JWTRefreshTokenRotationExample.AuthService.TokenPair originalPair = authService.login(username);

        // Act
        Optional<JWTRefreshTokenRotationExample.AuthService.TokenPair> result = 
                authService.refresh(originalPair.refresh().id());

        // Assert
        assertTrue("Refresh should succeed with valid token", result.isPresent());
        assertNotNull("New token pair should not be null", result.get());
        assertNotEquals("New refresh token should be different from original",
                        originalPair.refresh().id(), result.get().refresh().id());
    }

    @Test
    public void testRefresh_WithInvalidToken_ReturnsEmpty() {
        // Arrange
        String invalidRefreshToken = "invalid-token-id";

        // Act
        Optional<JWTRefreshTokenRotationExample.AuthService.TokenPair> result = 
                authService.refresh(invalidRefreshToken);

        // Assert
        assertFalse("Refresh should fail with invalid token", result.isPresent());
    }

    @Test
    public void testReplayAttack_RevokesSession() {
        // Arrange
        String username = "testuser";
        JWTRefreshTokenRotationExample.AuthService.TokenPair originalPair = authService.login(username);
        
        // First refresh - should succeed
        Optional<JWTRefreshTokenRotationExample.AuthService.TokenPair> firstRefresh = 
                authService.refresh(originalPair.refresh().id());
        assertTrue("First refresh should succeed", firstRefresh.isPresent());

        // Act - Attempt to reuse the original refresh token (replay attack)
        Optional<JWTRefreshTokenRotationExample.AuthService.TokenPair> replayAttempt = 
                authService.refresh(originalPair.refresh().id());

        // Assert
        assertFalse("Replay attempt should fail", replayAttempt.isPresent());
    }

    @Test
    public void testReplayAttack_InvalidatesFutureRefreshes() {
        // Arrange
        String username = "testuser";
        JWTRefreshTokenRotationExample.AuthService.TokenPair pair1 = authService.login(username);
        Optional<JWTRefreshTokenRotationExample.AuthService.TokenPair> pair2 = 
                authService.refresh(pair1.refresh().id());
        
        // Trigger replay attack
        authService.refresh(pair1.refresh().id());

        // Act - Try to use the legitimate token from pair2
        Optional<JWTRefreshTokenRotationExample.AuthService.TokenPair> result = 
                authService.refresh(pair2.get().refresh().id());

        // Assert
        assertFalse("Session should be revoked, all tokens invalid", result.isPresent());
    }

    @Test
    public void testMultipleSequentialRefreshes_Success() {
        // Arrange
        String username = "testuser";
        JWTRefreshTokenRotationExample.AuthService.TokenPair pair1 = authService.login(username);

        // Act - Perform multiple sequential refreshes
        Optional<JWTRefreshTokenRotationExample.AuthService.TokenPair> pair2 = 
                authService.refresh(pair1.refresh().id());
        Optional<JWTRefreshTokenRotationExample.AuthService.TokenPair> pair3 = 
                authService.refresh(pair2.get().refresh().id());
        Optional<JWTRefreshTokenRotationExample.AuthService.TokenPair> pair4 = 
                authService.refresh(pair3.get().refresh().id());

        // Assert
        assertTrue("Second refresh should succeed", pair2.isPresent());
        assertTrue("Third refresh should succeed", pair3.isPresent());
        assertTrue("Fourth refresh should succeed", pair4.isPresent());
        
        // All refresh tokens should be different
        assertNotEquals(pair1.refresh().id(), pair2.get().refresh().id());
        assertNotEquals(pair2.get().refresh().id(), pair3.get().refresh().id());
        assertNotEquals(pair3.get().refresh().id(), pair4.get().refresh().id());
    }

    @Test
    public void testAccessToken_HasExpirationTime() {
        // Arrange & Act
        JWTRefreshTokenRotationExample.AuthService.TokenPair pair = authService.login("user");

        // Assert
        assertNotNull("Access token expiration should not be null", pair.access().exp());
        assertTrue("Access token should expire in the future", 
                   pair.access().exp().isAfter(Instant.now()));
    }

    @Test
    public void testRefreshToken_HasExpirationTime() {
        // Arrange & Act
        JWTRefreshTokenRotationExample.AuthService.TokenPair pair = authService.login("user");

        // Assert
        assertNotNull("Refresh token expiration should not be null", pair.refresh().exp());
        assertTrue("Refresh token should expire in the future", 
                   pair.refresh().exp().isAfter(Instant.now()));
    }

    @Test
    public void testLogin_DifferentUsers_GetDifferentTokens() {
        // Arrange & Act
        JWTRefreshTokenRotationExample.AuthService.TokenPair user1Pair = authService.login("user1");
        JWTRefreshTokenRotationExample.AuthService.TokenPair user2Pair = authService.login("user2");

        // Assert
        assertNotEquals("Different users should have different refresh tokens",
                        user1Pair.refresh().id(), user2Pair.refresh().id());
        assertNotEquals("Different users should have different access tokens",
                        user1Pair.access().jwt(), user2Pair.access().jwt());
    }

    @Test
    public void testRefresh_GeneratesNewAccessToken() {
        // Arrange
        JWTRefreshTokenRotationExample.AuthService.TokenPair originalPair = authService.login("user");
        String originalAccessToken = originalPair.access().jwt();

        // Act
        Optional<JWTRefreshTokenRotationExample.AuthService.TokenPair> refreshedPair = 
                authService.refresh(originalPair.refresh().id());

        // Assert
        assertTrue(refreshedPair.isPresent());
        assertNotEquals("New access token should be different from original",
                        originalAccessToken, refreshedPair.get().access().jwt());
    }
}
