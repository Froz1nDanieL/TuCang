package com.mushan.tucangbackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.exception.ThrowUtils;
import com.mushan.tucangbackend.utils.ImageContentValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class FilePictureUpload extends PictureUploadTemplate {
  
    @Override  
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 1. 校验文件大小  
        long fileSize = multipartFile.getSize();  
        final long ONE_M = 1024 * 1024L;  
        ThrowUtils.throwIf(fileSize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");  
        // 2. 校验文件后缀  
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        fileSuffix = fileSuffix == null ? "" : fileSuffix.toLowerCase(Locale.ROOT);
        // 允许上传的文件后缀  
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
        String contentType = multipartFile.getContentType();
        ThrowUtils.throwIf(contentType != null && !Arrays.asList(
                        "image/jpeg", "image/jpg", "image/png", "image/webp"
                ).contains(contentType.toLowerCase(Locale.ROOT)),
                ErrorCode.PARAMS_ERROR, "文件 MIME 类型错误");
        try {
            ImageContentValidator.validate(multipartFile.getInputStream(), fileSuffix);
        } catch (java.io.IOException exception) {
            throw new com.mushan.tucangbackend.exception.BusinessException(
                    ErrorCode.PARAMS_ERROR, "无法读取图片文件");
        }
    }  
  
    @Override  
    protected String getOriginFilename(Object inputSource) {  
        MultipartFile multipartFile = (MultipartFile) inputSource;  
        return multipartFile.getOriginalFilename();  
    }  
  
    @Override  
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;  
        multipartFile.transferTo(file);
        ImageContentValidator.validateFile(file, FileUtil.getSuffix(multipartFile.getOriginalFilename()));
    }  
}
