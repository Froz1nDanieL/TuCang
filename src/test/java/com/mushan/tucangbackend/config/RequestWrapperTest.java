package com.mushan.tucangbackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mushan.tucangbackend.model.dto.user.UserEditRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestWrapperTest {

    @Test
    void shouldPreserveUtf8JsonAcrossRepeatedReads() throws Exception {
        String json = "{\"userName\":\"中文昵称\",\"userProfile\":\"你好，世界\"}";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json;charset=UTF-8");
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContent(json.getBytes(StandardCharsets.UTF_8));

        RequestWrapper wrapper = new RequestWrapper(request);
        String firstRead = StreamUtils.copyToString(wrapper.getInputStream(), StandardCharsets.UTF_8);
        String secondRead = wrapper.getReader().lines().collect(Collectors.joining());
        UserEditRequest editRequest = new ObjectMapper().readValue(
                wrapper.getInputStream(), UserEditRequest.class);

        assertEquals(json, firstRead);
        assertEquals(json, secondRead);
        assertEquals(json, wrapper.getBody());
        assertEquals("中文昵称", editRequest.getUserName());
        assertEquals("你好，世界", editRequest.getUserProfile());
    }
}
