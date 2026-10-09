package com.fitcart.backend.dao;

import com.fitcart.backend.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserDaoTest {

    @Autowired
    private UserDao userDao;

    @Autowired
    private javax.sql.DataSource dataSource;

    @org.junit.jupiter.api.BeforeEach
    void verifyDatabaseAccess() {
        try (java.sql.Connection conn = dataSource.getConnection()) {
            org.junit.jupiter.api.Assumptions.assumeTrue(conn != null, "Connected to MySQL");
        } catch (Exception e) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false,
                "Skipping test: MySQL database not accessible with current credentials. Configure SPRING_DATASOURCE_PASSWORD to run.");
        }
    }

    @Test
    void testFindAllAndSaveUser() {
        assertNotNull(userDao, "UserDao should be injected");

        List<User> initialUsers = userDao.findAll();
        assertNotNull(initialUsers, "Initial users list should not be null");

        String testEmail = "test_" + System.currentTimeMillis() + "@fitcart.com";
        User testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail(testEmail);
        testUser.setPassword("securePassword123");
        testUser.setRole("BUYER");
        testUser.setPhone("9876543210");

        User savedUser = userDao.save(testUser);
        assertNotNull(savedUser.getUserId(), "Saved user should have an auto-generated userId");

        Optional<User> fetched = userDao.findById(savedUser.getUserId());
        assertTrue(fetched.isPresent(), "Saved user should be found by ID");
        assertEquals("Test User", fetched.get().getName());
        assertEquals(testEmail, fetched.get().getEmail());

        // Clean up test user to preserve database integrity
        int deleted = userDao.deleteById(savedUser.getUserId());
        assertEquals(1, deleted, "Test user should be cleaned up");
    }
}
