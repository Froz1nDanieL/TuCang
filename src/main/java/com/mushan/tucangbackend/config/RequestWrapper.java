package com.mushan.tucangbackend.config;

import org.springframework.util.StreamUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

/**
 * 包装请求，使 InputStream 可以重复读取
 *
 * @author pine
 */
public class RequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body;

    public RequestWrapper(HttpServletRequest request) {
        super(request);
        try {
            // 原样缓存请求字节，避免使用系统默认字符集进行 String/byte[] 往返转换
            this.body = StreamUtils.copyToByteArray(request.getInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException("读取请求体失败", e);
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }

            @Override
            public int read() throws IOException {
                return byteArrayInputStream.read();
            }

            @Override
            public int read(byte[] bytes, int offset, int length) {
                return byteArrayInputStream.read(bytes, offset, length);
            }
        };

    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(this.getInputStream(), getRequestCharset()));
    }

    public String getBody() {
        return new String(this.body, getRequestCharset());
    }

    private Charset getRequestCharset() {
        String characterEncoding = getCharacterEncoding();
        if (characterEncoding == null) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(characterEncoding);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            return StandardCharsets.UTF_8;
        }
    }

}
