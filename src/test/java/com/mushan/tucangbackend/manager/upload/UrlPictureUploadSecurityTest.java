package com.mushan.tucangbackend.manager.upload;

import com.mushan.tucangbackend.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlPictureUploadSecurityTest {

    private final UrlPictureUpload upload = new UrlPictureUpload();

    @Test
    void blocksLoopbackAndMetadataAddresses() throws Exception {
        assertBlocked("http://127.0.0.1/image.png");
        assertBlocked("http://169.254.169.254/latest/meta-data");
        assertBlocked("http://[::1]/image.png");
    }

    @Test
    void blocksCredentialsUnsupportedPortsAndProtocols() throws Exception {
        assertBlocked("http://user:password@example.com/image.png");
        assertBlocked("http://example.com:8080/image.png");
        assertBlocked("file:///etc/passwd");
    }

    private void assertBlocked(String value) throws Exception {
        Method method = UrlPictureUpload.class.getDeclaredMethod("validatePublicUrl", URL.class);
        method.setAccessible(true);
        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(upload, new URL(value))
        );
        assertTrue(exception.getCause() instanceof BusinessException);
    }
}
