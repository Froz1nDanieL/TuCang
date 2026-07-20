package com.mushan.tucangbackend.utils;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRequestContentTypeUtilsTest {

    @Test
    void shouldRecognizeJsonWithCharset() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json;charset=UTF-8");

        assertTrue(HttpRequestContentTypeUtils.isJsonRequest(request));
    }

    @Test
    void shouldRejectNonJsonAndInvalidContentType() {
        MockHttpServletRequest textRequest = new MockHttpServletRequest();
        textRequest.setContentType("text/plain");
        MockHttpServletRequest invalidRequest = new MockHttpServletRequest();
        invalidRequest.setContentType("not a media type");

        assertFalse(HttpRequestContentTypeUtils.isJsonRequest(textRequest));
        assertFalse(HttpRequestContentTypeUtils.isJsonRequest(invalidRequest));
    }
}
