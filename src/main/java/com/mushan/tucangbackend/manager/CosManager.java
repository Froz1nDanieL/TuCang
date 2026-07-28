package com.mushan.tucangbackend.manager;

import cn.hutool.core.io.FileUtil;
import com.mushan.tucangbackend.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.ciModel.common.ImageProcessRequest;
import com.qcloud.cos.model.ciModel.persistence.CIUploadResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import cn.hutool.core.util.StrUtil;

@Component
public class CosManager {  
  
    @Resource
    private CosClientConfig cosClientConfig;
  
    @Resource  
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key 唯一键
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传对象（附带图片信息）
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 对图片进行处理（获取基本信息也被视作为一种处理）
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息
        picOperations.setIsPicInfo(1);
        List<PicOperations.Rule> rules = new ArrayList<>();
        // 图片压缩（转成 webp 格式）
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setRule("imageMogr2/format/webp");
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setFileId(webpKey);
        rules.add(compressRule);
        // 缩略图处理，仅对 > 20 KB 的图片生成缩略图
        if (file.length() > 20 * 1024) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnailRule.setFileId(thumbnailKey);
            // 缩放规则 /thumbnail/<Width>x<Height>>（如果大于原图宽高，则不处理）
            // 提高缩略图质量，从原来的128x128改为512x512
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 512, 512));
            rules.add(thumbnailRule);
        }
        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除对象
     *
     * @param key 文件 key
     */
    public void deleteObject(String key) throws CosClientException {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }

    /**
     * 获取对象存储域名
     *
     * @return 域名
     */
    public String getHost() {
        return cosClientConfig.getHost();
    }

    public boolean isManagedUrl(String url) {
        if (StrUtil.isBlank(url) || StrUtil.isBlank(cosClientConfig.getHost())) {
            return false;
        }
        String host = cosClientConfig.getHost().replaceAll("/+$", "");
        return url.equals(host) || url.startsWith(host + "/");
    }

    public ObjectMetadata getObjectMetadataByUrl(String url) {
        return cosClient.getObjectMetadata(cosClientConfig.getBucket(), extractKey(url));
    }

    public boolean isAvailable() {
        if (StrUtil.hasBlank(cosClientConfig.getBucket(), cosClientConfig.getRegion(),
                cosClientConfig.getSecretId(), cosClientConfig.getSecretKey())) {
            return false;
        }
        return cosClient.doesBucketExist(cosClientConfig.getBucket());
    }

    public String regenerateThumbnail(String url) {
        String key = extractKey(url);
        String suffix = FileUtil.getSuffix(key);
        int slashIndex = key.lastIndexOf('/');
        String directory = slashIndex < 0 ? "" : key.substring(0, slashIndex + 1);
        String fileName = slashIndex < 0 ? key : key.substring(slashIndex + 1);
        String thumbnailKey = directory + FileUtil.mainName(fileName) + "_thumbnail."
                + (suffix == null || suffix.isEmpty() ? "jpg" : suffix);
        PicOperations operations = new PicOperations();
        operations.setIsPicInfo(1);
        PicOperations.Rule rule = new PicOperations.Rule();
        rule.setBucket(cosClientConfig.getBucket());
        rule.setFileId(thumbnailKey);
        rule.setRule("imageMogr2/thumbnail/512x512>");
        operations.setRules(Collections.singletonList(rule));
        ImageProcessRequest request = new ImageProcessRequest(cosClientConfig.getBucket(), key);
        request.setPicOperations(operations);
        CIUploadResult ignored = cosClient.processImage(request);
        return cosClientConfig.getHost().replaceAll("/+$", "") + "/" + thumbnailKey;
    }

    private String extractKey(String url) {
        if (!isManagedUrl(url)) {
            throw new IllegalArgumentException("对象不属于当前 COS 域名");
        }
        String host = cosClientConfig.getHost().replaceAll("/+$", "");
        String key = url.substring(host.length());
        while (key.startsWith("/")) {
            key = key.substring(1);
        }
        int queryIndex = key.indexOf('?');
        return queryIndex < 0 ? key : key.substring(0, queryIndex);
    }

}
