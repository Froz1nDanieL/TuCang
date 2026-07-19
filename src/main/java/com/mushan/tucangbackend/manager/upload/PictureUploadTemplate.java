package com.mushan.tucangbackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mushan.tucangbackend.config.CosClientConfig;
import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.manager.CosManager;
import com.mushan.tucangbackend.model.color.ColorAnalysisResult;
import com.mushan.tucangbackend.model.dto.file.UploadPictureResult;
import com.mushan.tucangbackend.utils.ColorPaletteUtils;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class PictureUploadTemplate {
  
    @Resource  
    protected CosManager cosManager;
  
    @Resource
    protected CosClientConfig cosClientConfig;
  
    /**  
     * 模板方法，定义上传流程  
     */  
    public final UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1. 校验图片
        validPicture(inputSource);

        // 2. 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originFilename = getOriginFilename(inputSource);
        // 获取文件扩展名
        String suffix = StrUtil.blankToDefault(FileUtil.getSuffix(originFilename), "jpg");
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, suffix);
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);  
  
        File file = null;
        try {
            // 创建临时文件，使用安全的前缀和后缀
            String tempPrefix = "upload_" + uuid;
            // 确保前缀不超过Java允许的最大长度(160个字符减去后缀长度)
            if (tempPrefix.length() > 150) {
                tempPrefix = tempPrefix.substring(0, 150);
            }
            // 确保后缀以点开头且只包含合法字符
            String tempSuffix = "." + suffix.replaceAll("[^a-zA-Z0-9]", "jpg");
            file = File.createTempFile(tempPrefix, tempSuffix);
            // 处理文件来源（本地或 URL）
            processFile(inputSource, file);
            // 上传图片到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            ColorAnalysisResult colorAnalysisResult = analyzeColor(file, imageInfo);
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                CIObject compressedCiObject = objectList.get(0);
                // 缩略图默认等于压缩图
                CIObject thumbnailCiObject = compressedCiObject;
                // 有生成缩略图，才得到缩略图
                if (objectList.size() > 1) {
                    thumbnailCiObject = objectList.get(1);
                }
                // 封装压缩图返回结果
                return buildResult(originFilename, compressedCiObject, thumbnailCiObject, imageInfo, colorAnalysisResult);
            }
            // 封装原图返回结果
            return buildResult(originFilename, file, uploadPath, imageInfo, colorAnalysisResult);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 6. 清理临时文件  
            deleteTempFile(file);
        }  
    }  
  
    /**  
     * 校验输入源（本地文件或 URL）  
     */  
    protected abstract void validPicture(Object inputSource);
  
    /**  
     * 获取输入源的原始文件名  
     */  
    protected abstract String getOriginFilename(Object inputSource);  
  
    /**  
     * 处理输入源并生成本地临时文件  
     */  
    protected abstract void processFile(Object inputSource, File file) throws Exception;


    /**
     * 封装压缩图返回结果
     *
     * @param compressedCiObject(图片处理后的结果)
     * @param originFilename
     * @param thumbnailCiObject
     * @return
     */

    private UploadPictureResult buildResult(String originFilename, CIObject compressedCiObject,
                                            CIObject thumbnailCiObject, ImageInfo imageInfo,
                                            ColorAnalysisResult colorAnalysisResult) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        fillColorResult(uploadPictureResult, imageInfo, colorAnalysisResult);
        // 设置图片为缩略图的地址，去除可能包含的敏感参数
        String thumbnailUrl = cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey();
        uploadPictureResult.setThumbnailUrl(removeSensitiveParams(thumbnailUrl));
        // 设置图片为压缩后的地址，去除可能包含的敏感参数
        String imageUrl = cosClientConfig.getHost() + "/" + compressedCiObject.getKey();
        uploadPictureResult.setUrl(removeSensitiveParams(imageUrl));
        return uploadPictureResult;
    }

    /**  
     * 封装返回结果  
     */  
    private UploadPictureResult buildResult(String originFilename, File file, String uploadPath,
                                            ImageInfo imageInfo, ColorAnalysisResult colorAnalysisResult) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = imageInfo.getWidth();  
        int picHeight = imageInfo.getHeight();  
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicSize(FileUtil.size(file));
        fillColorResult(uploadPictureResult, imageInfo, colorAnalysisResult);
        String imageUrl = cosClientConfig.getHost() + "/" + uploadPath;
        uploadPictureResult.setUrl(removeSensitiveParams(imageUrl));
        return uploadPictureResult;
    }  

    private ColorAnalysisResult analyzeColor(File file, ImageInfo imageInfo) {
        try {
            return ColorPaletteUtils.analyze(file);
        } catch (Exception analysisException) {
            log.warn("本地 Lab 调色板提取失败，降级使用对象存储平均色: {}", analysisException.getMessage());
            try {
                return ColorPaletteUtils.fromAverageColor(imageInfo.getAve());
            } catch (Exception fallbackException) {
                log.warn("平均色降级分析失败，本次上传不写入颜色索引: {}", fallbackException.getMessage());
                return null;
            }
        }
    }

    private void fillColorResult(UploadPictureResult result, ImageInfo imageInfo,
                                 ColorAnalysisResult colorAnalysisResult) {
        if (colorAnalysisResult == null) {
            result.setPicColor(imageInfo.getAve());
            return;
        }
        result.setPicColor(colorAnalysisResult.getDominantColor());
        result.setColorPalette(JSONUtil.toJsonStr(colorAnalysisResult.getPalette()));
        result.setColorTags(JSONUtil.toJsonStr(colorAnalysisResult.getColorTags()));
        result.setColorScores(JSONUtil.toJsonStr(colorAnalysisResult.getColorScores()));
        result.setColorAlgoVersion(colorAnalysisResult.getAlgorithmVersion());
    }
  
    /**
     * 移除URL中的敏感参数
     * @param url 包含敏感参数的URL
     * @return 去除敏感参数后的URL
     */
    private String removeSensitiveParams(String url) {
        if (StrUtil.isBlank(url)) {
            return url;
        }
        // 移除常见的敏感参数
        return url.split("\\?")[0];
    }
    
    /**  
     * 删除临时文件  
     */  
    public void deleteTempFile(File file) {  
        if (file == null) {  
            return;  
        }  
        boolean deleteResult = file.delete();  
        if (!deleteResult) {  
            log.error("file delete error, filepath = {}", file.getAbsolutePath());  
        }  
    }  
}
