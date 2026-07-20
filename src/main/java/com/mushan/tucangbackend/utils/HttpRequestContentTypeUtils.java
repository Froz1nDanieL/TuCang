package com.mushan.tucangbackend.utils;

import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletRequest;

/**
 * HTTP 请求 Content-Type 工具类。
 */
public final class HttpRequestContentTypeUtils {

    private HttpRequestContentTypeUtils() {
    }

    /**
     * 判断请求是否为 JSON。兼容 application/json;charset=UTF-8 等合法写法。
     */
    public static boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) {
            return false;
        }
        try {
            MediaType requestMediaType = MediaType.parseMediaType(contentType);
            return MediaType.APPLICATION_JSON.isCompatibleWith(requestMediaType);
        } catch (InvalidMediaTypeException e) {
            return false;
        }
    }
}
