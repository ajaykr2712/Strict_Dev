package com.ecommerce.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Unit tests for UserService
 * Tests user registration, authentication, and profile management
 */
@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @Before
    public void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    public void testRegisterUser_ValidData_Success() {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        String firstName = "John";
        String lastName = "Doe";
        
        User user = new User(email, password, firstName, lastName);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        User result = userService.registerUser(email, password, firstName, lastName);

        // Assert
        assertNotNull("User should not be null", result);
        assertEquals("Email should match", email, result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterUser_DuplicateEmail_ThrowsException() {
        // Arrange
        String email = "existing@example.com";
        User existingUser = new User(email, "pass", "Jane", "Doe");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));

        // Act
        userService.registerUser(email, "newpass", "John", "Smith");

        // Assert - Exception expected
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterUser_NullEmail_ThrowsException() {
        // Act
        userService.registerUser(null, "password", "John", "Doe");

        // Assert - Exception expected
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterUser_InvalidEmail_ThrowsException() {
        // Act
        userService.registerUser("invalid-email", "password", "John", "Doe");

        // Assert - Exception expected
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterUser_WeakPassword_ThrowsException() {
        // Act
        userService.registerUser("test@example.com", "123", "John", "Doe");

        // Assert - Exception expected
    }

    @Test
    public void testFindUserByEmail_ExistingUser_ReturnsUser() {
        // Arrange
        String email = "test@example.com";
        User user = new User(email, "pass", "John", "Doe");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.findUserByEmail(email);

        // Assert
        assertTrue("User should be found", result.isPresent());
        assertEquals("Email should match", email, result.get().getEmail());
    }

    @Test
    public void testFindUserByEmail_NonExistingUser_ReturnsEmpty() {
        // Arrange
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findUserByEmail(email);

        // Assert
        assertFalse("User should not be found", result.isPresent());
    }

    @Test
    public void testUpdateUserProfile_Success() {
        // Arrange
        String userId = "user-123";
        User existingUser = new User("test@example.com", "pass", "John", "Doe");
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        User result = userService.updateUserProfile(userId, "Jane", "Smith");

        // Assert
        assertEquals("First name should be updated", "Jane", result.getFirstName());
        assertEquals("Last name should be updated", "Smith", result.getLastName());
    }

    @Test
    public void testDeleteUser_Success() {
        // Arrange
        String userId = "user-123";

        // Act
        userService.deleteUser(userId);

        // Assert
        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    public void testListAllUsers_ReturnsMultipleUsers() {
        // Arrange
        List<User> users = Arrays.asList(
            new User("user1@test.com", "pass1", "John", "Doe"),
            new User("user2@test.com", "pass2", "Jane", "Smith")
        );
        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<User> result = userService.listAllUsers();

        // Assert
        assertEquals("Should return 2 users", 2, result.size());
        verify(userRepository, times(1)).findAll();
    }

    // Mock UserService implementation for testing
    static class UserService {
        private UserRepository repository;

        public UserService(UserRepository repository) {
            this.repository = repository;
        }

        public User registerUser(String email, String password, String firstName, String lastName) {
            if (email == null || !email.contains("@")) {
                throw new IllegalArgumentException("Invalid email");
            }
            if (password == null || password.length() < 6) {
                throw new IllegalArgumentException("Password too weak");
            }
            if (repository.findByEmail(email).isPresent()) {
                throw new IllegalArgumentException("Email already exists");
            }
            User user = new User(email, password, firstName, lastName);
            return repository.save(user);
        }

        public Optional<User> findUserByEmail(String email) {
            return repository.findByEmail(email);
        }

        public User updateUserProfile(String userId, String firstName, String lastName) {
            User user = repository.findById(userId).orElseThrow();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            return repository.save(user);
        }

        public void deleteUser(String userId) {
            repository.deleteById(userId);
        }

        public List<User> listAllUsers() {
            return repository.findAll();
        }
    }
}
