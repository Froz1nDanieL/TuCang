package com.mushan.tucangbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.exception.ThrowUtils;
import com.mushan.tucangbackend.manager.CosManager;
import com.mushan.tucangbackend.mapper.AdminOperationLogMapper;
import com.mushan.tucangbackend.mapper.PictureIndexRecordMapper;
import com.mushan.tucangbackend.mapper.UserPictureInteractionMapper;
import com.mushan.tucangbackend.model.entity.AdminOperationLog;
import com.mushan.tucangbackend.model.entity.AiGenHistory;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.entity.PictureIndexRecord;
import com.mushan.tucangbackend.model.entity.PictureReviewRecord;
import com.mushan.tucangbackend.model.entity.Space;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.entity.UserPictureInteraction;
import com.mushan.tucangbackend.model.vo.admin.AdminAssetTraceVO;
import com.mushan.tucangbackend.model.vo.admin.AdminIndexCheckVO;
import com.mushan.tucangbackend.model.vo.admin.AdminPictureVO;
import com.qcloud.cos.model.ObjectMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class AdminAssetService {

    @Resource private PictureService pictureService;
    @Resource private UserService userService;
    @Resource private SpaceService spaceService;
    @Resource private AiGenHistoryService aiGenHistoryService;
    @Resource private PictureReviewRecordService reviewRecordService;
    @Resource private PictureIndexRecordMapper indexRecordMapper;
    @Resource private AdminOperationLogMapper operationLogMapper;
    @Resource private UserPictureInteractionMapper interactionMapper;
    @Resource private PictureSearchIndexService searchIndexService;
    @Resource private PictureCacheService pictureCacheService;
    @Resource private PictureChangeNotifier pictureChangeNotifier;
    @Resource private CosManager cosManager;

    @Value("${tucang.admin.asset-diagnostic-timeout-ms:2000}")
    private long timeoutMs;

    public AdminAssetTraceVO trace(Long pictureId, Long operatorId) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        AdminAssetTraceVO vo = new AdminAssetTraceVO();
        AdminPictureVO pictureVO = AdminPictureVO.from(picture);
        User owner = userService.getById(picture.getUserId());
        Space space = picture.getSpaceId() == null ? null : spaceService.getById(picture.getSpaceId());
        pictureVO.setUserName(owner == null ? null : owner.getUserName());
        vo.setPicture(pictureVO);
        vo.setOwnerName(owner == null ? null : owner.getUserName());
        vo.setSpaceName(space == null ? null : space.getSpaceName());
        if (picture.getAiTaskId() != null) {
            AiGenHistory ai = aiGenHistoryService.lambdaQuery()
                    .eq(AiGenHistory::getTaskId, picture.getAiTaskId()).one();
            vo.setAiTaskId(picture.getAiTaskId());
            vo.setAiModelName(ai == null ? null : ai.getModelName());
            if (ai != null) {
                addTimeline(vo, "AI", "AI 任务创建", ai.getModelName(), ai.getCreateTime(), ai.getTaskStatus());
            }
        }
        addTimeline(vo, "PICTURE", "图片记录创建", picture.getName(),
                picture.getCreateTime(), "SUCCESS");
        for (PictureReviewRecord review : reviewRecordService.listByPicture(pictureId)) {
            addTimeline(vo, "REVIEW", review.getToStatus() == 1 ? "审核通过" : "审核拒绝",
                    review.getReviewMessage(), review.getCreateTime(), review.getConflict() == 1 ? "CONFLICT" : "SUCCESS");
        }
        List<PictureIndexRecord> indexRecords = indexRecordMapper.selectList(
                new QueryWrapper<PictureIndexRecord>().eq("pictureId", pictureId)
                        .orderByDesc("createTime").last("LIMIT 50"));
        for (PictureIndexRecord record : indexRecords) {
            addTimeline(vo, "INDEX", "ES " + safe(record.getOperation()),
                    record.getErrorMessage(), record.getCreateTime(), record.getSuccess() == 1 ? "SUCCESS" : "FAILED");
        }
        List<AdminOperationLog> logs = operationLogMapper.selectList(
                new QueryWrapper<AdminOperationLog>().eq("targetType", "picture")
                        .eq("targetId", String.valueOf(pictureId))
                        .orderByDesc("createTime").last("LIMIT 30"));
        for (AdminOperationLog log : logs) {
            addTimeline(vo, "ADMIN", log.getAction(), log.getErrorMessage(),
                    log.getCreateTime(), log.getSuccess() == 1 ? "SUCCESS" : "FAILED");
        }
        List<UserPictureInteraction> interactions = interactionMapper.selectList(
                new QueryWrapper<UserPictureInteraction>().eq("pictureId", pictureId)
                        .orderByAsc("createTime").last("LIMIT 2"));
        for (UserPictureInteraction interaction : interactions) {
            addTimeline(vo, "INTERACTION", interaction.getType() == 0 ? "获得点赞" : "加入收藏",
                    "用户 " + interaction.getUserId(), interaction.getCreateTime(), "SUCCESS");
        }

        CompletableFuture<AdminAssetTraceVO.Check> cos = CompletableFuture.supplyAsync(() -> checkCos(picture));
        CompletableFuture<AdminAssetTraceVO.Check> es = CompletableFuture.supplyAsync(() -> checkEs(pictureId, operatorId));
        CompletableFuture<AdminAssetTraceVO.Check> redis = CompletableFuture.supplyAsync(this::checkRedis);
        vo.getChecks().add(up("MySQL", "图片记录存在", "审核状态 " + picture.getReviewStatus()));
        vo.getChecks().add(await("COS", cos));
        vo.getChecks().add(await("Elasticsearch", es));
        vo.getChecks().add(await("Redis", redis));
        vo.getTimeline().sort(Comparator.comparing(
                AdminAssetTraceVO.TimelineItem::getTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return vo;
    }

    public AdminIndexCheckVO resync(Long pictureId, Long operatorId) {
        searchIndexService.upsertNow(pictureId, "MANUAL", operatorId);
        return searchIndexService.checkOne(pictureId, null, operatorId);
    }

    public String invalidateCache() {
        pictureCacheService.invalidate();
        return pictureCacheService.getVersion();
    }

    public String regenerateThumbnail(Long pictureId) {
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!cosManager.isManagedUrl(picture.getUrl()),
                ErrorCode.PARAMS_ERROR, "图片不属于当前 COS，无法重建缩略图");
        String thumbnailUrl = cosManager.regenerateThumbnail(picture.getUrl());
        Picture update = new Picture();
        update.setId(pictureId);
        update.setThumbnailUrl(thumbnailUrl);
        update.setEditTime(new Date());
        ThrowUtils.throwIf(!pictureService.updateById(update), ErrorCode.OPERATION_ERROR);
        pictureChangeNotifier.upsert(pictureId);
        return thumbnailUrl;
    }

    private AdminAssetTraceVO.Check checkCos(Picture picture) {
        if (!cosManager.isManagedUrl(picture.getUrl())) {
            return check("COS", "NOT_APPLICABLE", "外部对象", "URL 不属于当前 COS 域名");
        }
        try {
            ObjectMetadata metadata = cosManager.getObjectMetadataByUrl(picture.getUrl());
            boolean sizeMatches = picture.getPicSize() == null
                    || picture.getPicSize().longValue() == metadata.getContentLength();
            boolean typeMatches = contentTypeMatches(picture.getPicFormat(), metadata.getContentType());
            String status = sizeMatches && typeMatches ? "UP" : "MISMATCH";
            String summary = !sizeMatches ? "文件大小不一致"
                    : !typeMatches ? "Content-Type 不一致" : "对象存在";
            return check("COS", status, summary,
                    metadata.getContentType() + " · " + metadata.getContentLength() + " bytes");
        } catch (RuntimeException exception) {
            return check("COS", "UNAVAILABLE", "无法读取对象元数据", safe(exception.getMessage()));
        }
    }

    private AdminAssetTraceVO.Check checkEs(Long pictureId, Long operatorId) {
        AdminIndexCheckVO check = searchIndexService.checkOne(pictureId, null, operatorId);
        if (!"UP".equals(check.getStatus())) {
            return check("Elasticsearch", "UNAVAILABLE", "索引服务不可用", check.getMessage());
        }
        return check("Elasticsearch", check.isConsistent() ? "UP" : "MISMATCH",
                check.isConsistent() ? "索引一致" : "发现 " + check.getMismatchTypes().size() + " 项差异",
                String.join("、", check.getMismatchTypes()));
    }

    private AdminAssetTraceVO.Check checkRedis() {
        try {
            String version = pictureCacheService.getVersion();
            return up("Redis", "缓存版本可读", "当前全局版本 " + version);
        } catch (RuntimeException exception) {
            return check("Redis", "UNAVAILABLE", "缓存服务不可用", safe(exception.getMessage()));
        }
    }

    private AdminAssetTraceVO.Check await(String system, CompletableFuture<AdminAssetTraceVO.Check> future) {
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception exception) {
            future.cancel(true);
            return check(system, "UNAVAILABLE", "诊断超时", timeoutMs + " ms 内未返回");
        }
    }

    private AdminAssetTraceVO.Check up(String system, String summary, String detail) {
        return check(system, "UP", summary, detail);
    }

    private boolean contentTypeMatches(String format, String contentType) {
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            return false;
        }
        if (format == null || format.trim().isEmpty()) {
            return true;
        }
        String normalizedFormat = format.toLowerCase().replace("jpg", "jpeg");
        String normalizedType = contentType.toLowerCase().split(";")[0].replace("jpg", "jpeg");
        return normalizedType.endsWith("/" + normalizedFormat);
    }

    private AdminAssetTraceVO.Check check(String system, String status, String summary, String detail) {
        AdminAssetTraceVO.Check check = new AdminAssetTraceVO.Check();
        check.setSystem(system);
        check.setStatus(status);
        check.setSummary(summary);
        check.setDetail(detail);
        return check;
    }

    private void addTimeline(AdminAssetTraceVO vo, String type, String title,
                             String description, Date time, String status) {
        AdminAssetTraceVO.TimelineItem item = new AdminAssetTraceVO.TimelineItem();
        item.setType(type);
        item.setTitle(title);
        item.setDescription(description);
        item.setTime(time);
        item.setStatus(status);
        vo.getTimeline().add(item);
    }

    private String safe(String value) {
        return value == null ? null : value.substring(0, Math.min(value.length(), 512));
    }
}
