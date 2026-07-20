package com.mushan.tucangbackend.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServicePasswordTest {

    private final UserServiceImpl userService = new UserServiceImpl();

    @Test
    void shouldHashAndVerifyPasswordWithRandomSalt() {
        String password = "testPassword123";

        String firstHash = userService.getEncryptPassword(password);
        String secondHash = userService.getEncryptPassword(password);

        assertNotEquals(firstHash, secondHash);
        assertTrue(userService.matchesPassword(password, firstHash));
        assertTrue(userService.matchesPassword(password, secondHash));
        assertFalse(userService.matchesPassword("wrongPassword123", firstHash));
    }

    @Test
    void shouldRejectInvalidStoredHash() {
        assertFalse(userService.matchesPassword("testPassword123", "invalid-hash"));
    }
}
