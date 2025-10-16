package com.mushan.tucangbackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpUtil;
import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * AI生成图片上传模板
 * 用于先将AI生成的图片下载到本地临时文件，再上传到对象存储
 */
@Slf4j
@Service
public class AiGenPictureUpload extends PictureUploadTemplate {
    
    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        ThrowUtils.throwIf(fileUrl == null || fileUrl.isEmpty(), ErrorCode.PARAMS_ERROR, "文件地址不能为空");
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        // 使用时间戳和随机数生成唯一文件名
        String extension = "jpg"; // AI生成的图片默认为jpg格式
        if (fileUrl.contains(".")) {
            extension = FileUtil.extName(fileUrl);
            if (extension == null || extension.isEmpty()) {
                extension = "jpg";
            }
        }
        return System.currentTimeMillis() + "_" + RandomUtil.randomString(6) + "." + extension;
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;
        try {
            // 下载文件到临时目录
            HttpUtil.downloadFile(fileUrl, file);
            // 验证下载的文件是否有效
            if (!file.exists() || file.length() == 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "下载的AI生成图片为空或无效");
            }
        } catch (Exception e) {
            log.error("下载AI生成图片失败: {}", fileUrl, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "下载AI生成图片失败: " + e.getMessage());
        }
    }
}