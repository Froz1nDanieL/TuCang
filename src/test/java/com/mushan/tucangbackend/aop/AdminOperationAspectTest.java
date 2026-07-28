package com.mushan.tucangbackend.aop;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminOperationAspectTest {

    @Test
    void masksSensitiveFieldsAndTruncatesLargeText() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userPassword", "plain-password");
        payload.put("accessToken", "plain-token");
        payload.put("description", repeat("x", 3000));

        Method method = AdminOperationAspect.class.getDeclaredMethod("safeParams", Object[].class);
        method.setAccessible(true);
        String params = (String) method.invoke(
                new AdminOperationAspect(),
                new Object[]{new Object[]{payload}}
        );

        assertFalse(params.contains("plain-password"));
        assertFalse(params.contains("plain-token"));
        assertTrue(params.contains("***"));
        assertTrue(params.length() <= 2000);
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int index = 0; index < count; index++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
