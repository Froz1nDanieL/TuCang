package com.mushan.tucangbackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.utils.ImageContentValidator;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class UrlPictureUpload extends PictureUploadTemplate {

    private static final long MAX_BYTES = 2 * 1024 * 1024L;
    private static final int MAX_REDIRECTS = 5;
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        if (StrUtil.isBlank(fileUrl)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址不能为空");
        }
        try {
            validatePublicUrl(new URL(fileUrl));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片地址无效或不可访问");
        }
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        try {
            String name = FileUtil.getName(new URL((String) inputSource).getPath());
            return StrUtil.isBlank(name) ? "remote-picture.jpg" : name;
        } catch (Exception ignored) {
            return "remote-picture.jpg";
        }
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        URL current = new URL((String) inputSource);
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            validatePublicUrl(current);
            HttpURLConnection connection = (HttpURLConnection) current.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "TuCang-ImageFetcher/1.0");
            int status = connection.getResponseCode();
            if (status >= 300 && status < 400) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (StrUtil.isBlank(location) || redirect == MAX_REDIRECTS) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片地址重定向次数过多");
                }
                current = new URL(current, location);
                continue;
            }
            if (status < 200 || status >= 300) {
                connection.disconnect();
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "远程图片请求失败");
            }
            String contentType = connection.getContentType();
            if (contentType != null) {
                contentType = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
            }
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
                connection.disconnect();
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "远程文件不是受支持的图片");
            }
            long contentLength = connection.getContentLengthLong();
            if (contentLength > MAX_BYTES) {
                connection.disconnect();
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片大小不能超过 2MB");
            }
            try (InputStream input = connection.getInputStream();
                 FileOutputStream output = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                long total = 0;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    total += read;
                    if (total > MAX_BYTES) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片大小不能超过 2MB");
                    }
                    output.write(buffer, 0, read);
                }
            } finally {
                connection.disconnect();
            }
            if (file.length() == 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "远程图片内容为空");
            }
            ImageContentValidator.validateFile(file, suffixForContentType(contentType));
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "远程图片下载失败");
    }

    private void validatePublicUrl(URL url) throws IOException {
        if (!"http".equalsIgnoreCase(url.getProtocol()) && !"https".equalsIgnoreCase(url.getProtocol())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 地址");
        }
        if (url.getUserInfo() != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片地址不能包含用户凭据");
        }
        int port = url.getPort();
        if (port != -1 && port != 80 && port != 443) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片地址端口不受支持");
        }
        InetAddress[] addresses = InetAddress.getAllByName(url.getHost());
        if (addresses.length == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片域名无法解析");
        }
        for (InetAddress address : addresses) {
            byte[] raw = address.getAddress();
            boolean uniqueLocalV6 = raw.length == 16 && (raw[0] & 0xFE) == 0xFC;
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isMulticastAddress()
                    || uniqueLocalV6) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "禁止访问内网图片地址");
            }
        }
    }

    private String suffixForContentType(String contentType) {
        if ("image/png".equals(contentType)) {
            return "png";
        }
        if ("image/webp".equals(contentType)) {
            return "webp";
        }
        return "jpg";
    }
}
