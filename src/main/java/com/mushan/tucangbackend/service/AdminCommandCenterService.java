package com.mushan.tucangbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mushan.tucangbackend.constant.AdminPermissionConstant;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.exception.ThrowUtils;
import com.mushan.tucangbackend.manager.CosManager;
import com.mushan.tucangbackend.mapper.AdminOperationLogMapper;
import com.mushan.tucangbackend.mapper.PictureIndexRecordMapper;
import com.mushan.tucangbackend.model.entity.AdminOperationLog;
import com.mushan.tucangbackend.model.entity.AiGenHistory;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.entity.PictureIndexRecord;
import com.mushan.tucangbackend.model.entity.Space;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.vo.admin.AdminCommandCenterVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class AdminCommandCenterService {

    @Resource private PictureService pictureService;
    @Resource private AiGenHistoryService aiGenHistoryService;
    @Resource private SpaceService spaceService;
    @Resource private AdminMonitorService monitorService;
    @Resource private AdminPermissionService permissionService;
    @Resource private PictureIndexRecordMapper indexRecordMapper;
    @Resource private AdminOperationLogMapper operationLogMapper;
    @Resource private CosManager cosManager;

    @Value("${tucang.admin.review-sla-minutes:30}")
    private long reviewSlaMinutes;

    public AdminCommandCenterVO overview(int days, User user) {
        ThrowUtils.throwIf(days != 7 && days != 30, ErrorCode.PARAMS_ERROR);
        boolean admin = permissionService.hasPermission(user, AdminPermissionConstant.AI_TASK_VIEW);
        AdminCommandCenterVO vo = new AdminCommandCenterVO();
        vo.setDays(days);
        long pending = pictureService.lambdaQuery().eq(Picture::getReviewStatus, 0).count();
        Picture oldest = pictureService.lambdaQuery().eq(Picture::getReviewStatus, 0)
                .orderByAsc(Picture::getCreateTime).last("LIMIT 1").one();
        long oldestSeconds = oldest == null ? 0
                : Math.max(0L, (System.currentTimeMillis() - oldest.getCreateTime().getTime()) / 1000);
        vo.getStatuses().add(status("review", "审核队列",
                oldestSeconds > reviewSlaMinutes * 60 ? "WARNING" : "UP",
                String.valueOf(pending), "最老等待 " + formatDuration(oldestSeconds),
                "/content/reviews?overdue=true"));
        if (admin) {
            Date hourAgo = new Date(System.currentTimeMillis() - 60L * 60 * 1000);
            long ended = aiGenHistoryService.lambdaQuery().ge(AiGenHistory::getCreateTime, hourAgo)
                    .in(AiGenHistory::getTaskStatus, "SUCCEEDED", "FAILED", "CANCELED").count();
            long failed = aiGenHistoryService.lambdaQuery().ge(AiGenHistory::getCreateTime, hourAgo)
                    .in(AiGenHistory::getTaskStatus, "FAILED", "CANCELED").count();
            long running = aiGenHistoryService.lambdaQuery()
                    .in(AiGenHistory::getTaskStatus, "PENDING", "RUNNING", "UNKNOWN").count();
            double failureRate = ended == 0 ? 0 : failed * 100.0 / ended;
            vo.getStatuses().add(status("ai", "AI 任务",
                    failureRate >= 15 ? "WARNING" : "UP",
                    String.valueOf(running), String.format("近 1 小时失败率 %.1f%%", failureRate),
                    "/intelligence/ai-tasks?status=FAILED"));
            long mismatch = indexRecordMapper.selectCount(new QueryWrapper<PictureIndexRecord>()
                    .eq("recordType", "CHECK").eq("resolved", 0).eq("success", 1));
            PictureIndexRecord last = indexRecordMapper.selectOne(new QueryWrapper<PictureIndexRecord>()
                    .orderByDesc("createTime").last("LIMIT 1"));
            vo.getStatuses().add(status("index", "搜索索引",
                    mismatch > 0 ? "WARNING" : "UP", String.valueOf(mismatch),
                    last == null ? "尚未执行一致性检查" : "最近检查 " + last.getCreateTime(),
                    "/intelligence/search-quality"));
        }
        Map<String, String> dependencies = monitorService.getDependencyStatus();
        String health = dependencies.values().stream().allMatch("UP"::equals) ? "UP" : "WARNING";
        String cos = "UNKNOWN";
        try {
            cos = cosManager.isAvailable() ? "UP" : "DOWN";
        } catch (RuntimeException ignored) {
            cos = "UNAVAILABLE";
        }
        vo.getStatuses().add(status("system", "系统健康",
                "UP".equals(health) && "UP".equals(cos) ? "UP" : "WARNING",
                health, "COS " + cos, "/operations/health"));
        fillTrend(vo.getTrend(), days, admin);
        return vo;
    }

    public List<AdminCommandCenterVO.ActionItem> actions(int limit, User user) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        boolean admin = permissionService.hasPermission(user, AdminPermissionConstant.AI_TASK_VIEW);
        List<AdminCommandCenterVO.ActionItem> items = new ArrayList<>();
        Date deadline = new Date(System.currentTimeMillis() - reviewSlaMinutes * 60 * 1000);
        List<Picture> overdue = pictureService.lambdaQuery().eq(Picture::getReviewStatus, 0)
                .lt(Picture::getCreateTime, deadline).orderByAsc(Picture::getCreateTime)
                .last("LIMIT " + safeLimit).list();
        for (Picture picture : overdue) {
            items.add(action("REVIEW", "HIGH", "图片等待审核已超时",
                    picture.getName(), "/content/reviews?pictureId=" + picture.getId(),
                    picture.getCreateTime()));
        }
        if (admin) {
            List<PictureIndexRecord> mismatches = indexRecordMapper.selectList(
                    new QueryWrapper<PictureIndexRecord>().eq("recordType", "CHECK")
                            .eq("resolved", 0).eq("success", 1)
                            .orderByDesc("createTime").last("LIMIT " + safeLimit));
            for (PictureIndexRecord record : mismatches) {
                items.add(action("INDEX", "HIGH", "图片索引存在差异",
                        "图片 " + record.getPictureId(), "/content/assets/" + record.getPictureId(),
                        record.getCreateTime()));
            }
            List<AiGenHistory> failures = aiGenHistoryService.lambdaQuery()
                    .eq(AiGenHistory::getTaskStatus, "FAILED")
                    .orderByDesc(AiGenHistory::getCreateTime).last("LIMIT " + safeLimit).list();
            for (AiGenHistory task : failures) {
                items.add(action("AI", "MEDIUM", "AI 任务执行失败",
                        safe(task.getErrorMessage()), "/intelligence/ai-tasks?taskId=" + task.getId(),
                        task.getCreateTime()));
            }
            for (Space space : spaceService.list()) {
                double sizeRatio = ratio(space.getTotalSize(), space.getMaxSize());
                double countRatio = ratio(space.getTotalCount(), space.getMaxCount());
                if (Math.max(sizeRatio, countRatio) >= 0.85) {
                    items.add(action("SPACE", "MEDIUM", "空间容量接近上限",
                            space.getSpaceName() + " · " + Math.round(Math.max(sizeRatio, countRatio) * 100) + "%",
                            "/organization/spaces?spaceId=" + space.getId(), space.getUpdateTime()));
                }
            }
            Date fifteenMinutesAgo = new Date(System.currentTimeMillis() - 15L * 60 * 1000);
            List<AdminOperationLog> failedLogs = operationLogMapper.selectList(
                    new QueryWrapper<AdminOperationLog>().eq("success", 0)
                            .ge("createTime", fifteenMinutesAgo)
                            .orderByDesc("createTime").last("LIMIT " + safeLimit));
            for (AdminOperationLog log : failedLogs) {
                items.add(action("ADMIN", "MEDIUM", "管理操作失败",
                        log.getModule() + " / " + log.getAction(), "/operations/logs?success=0",
                        log.getCreateTime()));
            }
        }
        items.sort((a, b) -> {
            Date left = a.getTime();
            Date right = b.getTime();
            return right == null ? -1 : left == null ? 1 : right.compareTo(left);
        });
        return items.subList(0, Math.min(items.size(), safeLimit));
    }

    public List<AdminCommandCenterVO.ActionItem> events(int limit, User user, Date before) {
        List<AdminCommandCenterVO.ActionItem> events = actions(Math.min(50, Math.max(limit * 2, limit)), user);
        if (before != null) {
            events.removeIf(item -> item.getTime() == null || !item.getTime().before(before));
        }
        return events.subList(0, Math.min(events.size(), Math.max(1, Math.min(limit, 50))));
    }

    private void fillTrend(AdminCommandCenterVO.Trend trend, int days, boolean includeAi) {
        LocalDate today = LocalDate.now();
        for (int offset = days - 1; offset >= 0; offset--) {
            LocalDate day = today.minusDays(offset);
            Timestamp start = Timestamp.valueOf(day.atStartOfDay());
            Timestamp end = Timestamp.valueOf(day.plusDays(1).atStartOfDay());
            trend.getDates().add(day.format(DateTimeFormatter.ofPattern("MM-dd")));
            trend.getUploads().add(pictureService.lambdaQuery()
                    .ge(Picture::getCreateTime, start).lt(Picture::getCreateTime, end).count());
            trend.getReviewPassed().add(pictureService.lambdaQuery()
                    .eq(Picture::getReviewStatus, 1)
                    .ge(Picture::getReviewTime, start).lt(Picture::getReviewTime, end).count());
            trend.getReviewRejected().add(pictureService.lambdaQuery()
                    .eq(Picture::getReviewStatus, 2)
                    .ge(Picture::getReviewTime, start).lt(Picture::getReviewTime, end).count());
            trend.getAiTasks().add(includeAi ? aiGenHistoryService.lambdaQuery()
                    .ge(AiGenHistory::getCreateTime, start).lt(AiGenHistory::getCreateTime, end).count() : 0L);
        }
    }

    private AdminCommandCenterVO.StatusItem status(String key, String label, String state,
                                                    String value, String description, String target) {
        AdminCommandCenterVO.StatusItem item = new AdminCommandCenterVO.StatusItem();
        item.setKey(key); item.setLabel(label); item.setStatus(state); item.setValue(value);
        item.setDescription(description); item.setTargetPath(target);
        return item;
    }

    private AdminCommandCenterVO.ActionItem action(String type, String severity, String title,
                                                    String description, String target, Date time) {
        AdminCommandCenterVO.ActionItem item = new AdminCommandCenterVO.ActionItem();
        item.setType(type); item.setSeverity(severity); item.setTitle(title);
        item.setDescription(description); item.setTargetPath(target); item.setTime(time);
        return item;
    }

    private double ratio(Long used, Long max) {
        return used == null || max == null || max <= 0 ? 0 : used * 1.0 / max;
    }

    private String formatDuration(long seconds) {
        if (seconds < 60) return seconds + " 秒";
        if (seconds < 3600) return seconds / 60 + " 分钟";
        return seconds / 3600 + " 小时";
    }

    private String safe(String value) {
        if (value == null || value.trim().isEmpty()) return "未提供错误摘要";
        return value.substring(0, Math.min(value.length(), 120));
    }
}
