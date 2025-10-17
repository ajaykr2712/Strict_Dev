import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for Builder Pattern Implementation
 * Tests fluent API, object construction, and validation
 */
public class BuilderPatternTest {

    @Test
    public void testBuilder_CreatesValidObject() {
        // Arrange & Act
        User user = new User.Builder("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .age(30)
                .build();

        // Assert
        assertNotNull("User should not be null", user);
        assertEquals("Email should match", "john@example.com", user.getEmail());
        assertEquals("First name should match", "John", user.getFirstName());
        assertEquals("Last name should match", "Doe", user.getLastName());
        assertEquals("Age should match", 30, user.getAge());
    }

    @Test
    public void testBuilder_WithRequiredFieldOnly() {
        // Arrange & Act
        User user = new User.Builder("jane@example.com").build();

        // Assert
        assertNotNull("User should not be null", user);
        assertEquals("Email should match", "jane@example.com", user.getEmail());
        assertNull("First name should be null", user.getFirstName());
        assertNull("Last name should be null", user.getLastName());
        assertEquals("Age should be 0", 0, user.getAge());
    }

    @Test
    public void testBuilder_FluentInterface() {
        // Act
        User user = new User.Builder("test@example.com")
                .firstName("Test")
                .lastName("User")
                .age(25)
                .phone("123-456-7890")
                .build();

        // Assert
        assertEquals("Phone should match", "123-456-7890", user.getPhone());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_NullEmail_ThrowsException() {
        // Act
        new User.Builder(null).build();

        // Assert - Exception expected
    }

    @Test
    public void testBuilder_MethodChaining() {
        // Act - Test that each method returns builder
        User.Builder builder = new User.Builder("test@test.com");
        User.Builder result = builder.firstName("Test");

        // Assert
        assertSame("Builder should return itself", builder, result);
    }

    // Test User class with Builder pattern
    static class User {
        private final String email;
        private final String firstName;
        private final String lastName;
        private final int age;
        private final String phone;

        private User(Builder builder) {
            this.email = builder.email;
            this.firstName = builder.firstName;
            this.lastName = builder.lastName;
            this.age = builder.age;
            this.phone = builder.phone;
        }

        public String getEmail() { return email; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public int getAge() { return age; }
        public String getPhone() { return phone; }

        static class Builder {
            private final String email;
            private String firstName;
            private String lastName;
            private int age;
            private String phone;

            public Builder(String email) {
                if (email == null) {
                    throw new IllegalArgumentException("Email cannot be null");
                }
                this.email = email;
            }

            public Builder firstName(String firstName) {
                this.firstName = firstName;
                return this;
            }

            public Builder lastName(String lastName) {
                this.lastName = lastName;
                return this;
            }

            public Builder age(int age) {
                this.age = age;
                return this;
            }

            public Builder phone(String phone) {
                this.phone = phone;
                return this;
            }

            public User build() {
                return new User(this);
            }
        }
    }
}
