package com.mushan.tucangbackend.model.entity;

import org.junit.jupiter.api.Test;

import java.io.ObjectStreamClass;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserSerializationCompatibilityTest {

    @Test
    void keepsCompatibilityWithExistingRedisSessions() {
        assertEquals(
                -4007983043812036024L,
                ObjectStreamClass.lookup(User.class).getSerialVersionUID()
        );
    }
}
