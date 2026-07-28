package com.mushan.tucangbackend.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushan.tucangbackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.mushan.tucangbackend.api.aliyunai.model.CreateTextToImageTaskResponse;
import com.mushan.tucangbackend.constant.AiTaskStatusConstant;
import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.exception.ThrowUtils;
import com.mushan.tucangbackend.model.dto.admin.AdminAiTaskQueryRequest;
import com.mushan.tucangbackend.model.dto.picture.CreatePictureOutPaintingRequest;
import com.mushan.tucangbackend.model.dto.picture.CreateTextToImageRequest;
import com.mushan.tucangbackend.model.entity.AiGenHistory;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.enums.AiGenerationTaskTypeEnum;
import com.mushan.tucangbackend.model.vo.admin.AdminAiTaskStatsVO;
import com.mushan.tucangbackend.model.vo.admin.AdminAiTaskVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminAiTaskService {

    @Resource private AiGenHistoryService aiGenHistoryService;
    @Resource private UserService userService;
    @Resource private PictureService pictureService;

    public Page<AdminAiTaskVO> page(AdminAiTaskQueryRequest request) {
        ThrowUtils.throwIf(request == null || request.getPageSize() <= 0 || request.getPageSize() > 100,
                ErrorCode.PARAMS_ERROR);
        QueryWrapper<AiGenHistory> query = new QueryWrapper<>();
        query.eq(StrUtil.isNotBlank(request.getTaskStatus()), "taskStatus", request.getTaskStatus())
                .eq(request.getTaskType() != null, "taskType", request.getTaskType())
                .eq(StrUtil.isNotBlank(request.getModelName()), "modelName", request.getModelName())
                .eq(request.getUserId() != null, "userId", request.getUserId());
        List<String> sortable = java.util.Arrays.asList("createTime", "completedTime", "durationMs", "taskStatus");
        String sortField = sortable.contains(request.getSortField()) ? request.getSortField() : "createTime";
        query.orderBy(true, "ascend".equals(request.getSortOrder()), sortField);
        Page<AiGenHistory> source = aiGenHistoryService.page(
                new Page<AiGenHistory>(request.getCurrent(), request.getPageSize()), query);
        Page<AdminAiTaskVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(toVos(source.getRecords(), false));
        return result;
    }

    public AdminAiTaskVO detail(Long id) {
        AiGenHistory task = aiGenHistoryService.getById(id);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR);
        if (AiTaskStatusConstant.RUNNING.equals(task.getTaskStatus())
                || AiTaskStatusConstant.PENDING.equals(task.getTaskStatus())
                || AiTaskStatusConstant.UNKNOWN.equals(task.getTaskStatus())) {
            refreshQuietly(task);
            task = aiGenHistoryService.getById(id);
        }
        return toVos(Collections.singletonList(task), true).get(0);
    }

    public AdminAiTaskStatsVO stats(int days) {
        ThrowUtils.throwIf(days != 7 && days != 30, ErrorCode.PARAMS_ERROR);
        Date start = new Date(System.currentTimeMillis() - days * 24L * 60 * 60 * 1000);
        List<AiGenHistory> tasks = aiGenHistoryService.lambdaQuery()
                .ge(AiGenHistory::getCreateTime, start).list();
        AdminAiTaskStatsVO vo = new AdminAiTaskStatsVO();
        vo.setTotalCount(tasks.size());
        List<Long> durations = new ArrayList<>();
        for (AiGenHistory task : tasks) {
            String status = task.getTaskStatus();
            if (AiTaskStatusConstant.SUCCEEDED.equals(status)) {
                vo.setSucceededCount(vo.getSucceededCount() + 1);
                if (task.getDurationMs() != null) durations.add(task.getDurationMs());
            } else if (AiTaskStatusConstant.FAILED.equals(status)
                    || AiTaskStatusConstant.CANCELED.equals(status)) {
                vo.setFailedCount(vo.getFailedCount() + 1);
            } else {
                vo.setRunningCount(vo.getRunningCount() + 1);
            }
        }
        long ended = vo.getSucceededCount() + vo.getFailedCount();
        vo.setSuccessRate(ended == 0 ? 0 : vo.getSucceededCount() * 1.0 / ended);
        Collections.sort(durations);
        vo.setP50DurationMs(percentile(durations, 0.50));
        vo.setP95DurationMs(percentile(durations, 0.95));
        return vo;
    }

    public AdminAiTaskVO retry(Long id) {
        AiGenHistory original = aiGenHistoryService.getById(id);
        ThrowUtils.throwIf(original == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!AiTaskStatusConstant.FAILED.equals(original.getTaskStatus())
                        && !AiTaskStatusConstant.CANCELED.equals(original.getTaskStatus()),
                ErrorCode.CONFLICT_ERROR, "仅失败或取消的任务可以重试");
        User owner = userService.getById(original.getUserId());
        ThrowUtils.throwIf(owner == null, ErrorCode.NOT_FOUND_ERROR, "任务用户不存在");
        String newTaskId;
        if (Integer.valueOf(AiGenerationTaskTypeEnum.TEXT_TO_IMAGE.getValue())
                .equals(original.getTaskType())) {
            CreateTextToImageRequest retryRequest = StrUtil.isBlank(original.getRequestParams())
                    ? new CreateTextToImageRequest()
                    : JSONUtil.toBean(original.getRequestParams(), CreateTextToImageRequest.class);
            if (StrUtil.isBlank(retryRequest.getPrompt())) {
                retryRequest.setPrompt(original.getPrompt());
            }
            CreateTextToImageTaskResponse response = pictureService.createTextToImageTask(retryRequest, owner);
            newTaskId = response.getOutput().getTaskId();
        } else {
            CreatePictureOutPaintingRequest retryRequest = StrUtil.isBlank(original.getRequestParams())
                    ? new CreatePictureOutPaintingRequest()
                    : JSONUtil.toBean(original.getRequestParams(), CreatePictureOutPaintingRequest.class);
            if (retryRequest.getPictureId() == null) {
                retryRequest.setPictureId(original.getSourcePictureId());
            }
            CreateOutPaintingTaskResponse response =
                    pictureService.createPictureOutPaintingTask(retryRequest, owner);
            newTaskId = response.getOutput().getTaskId();
        }
        AiGenHistory newTask = aiGenHistoryService.lambdaQuery()
                .eq(AiGenHistory::getTaskId, newTaskId).one();
        ThrowUtils.throwIf(newTask == null, ErrorCode.OPERATION_ERROR, "重试任务记录创建失败");
        AiGenHistory update = new AiGenHistory();
        update.setId(newTask.getId());
        update.setRetryFromTaskId(original.getTaskId());
        aiGenHistoryService.updateById(update);
        return detail(newTask.getId());
    }

    public void refreshQuietly(AiGenHistory task) {
        try {
            User owner = userService.getById(task.getUserId());
            if (owner == null) return;
            if (Integer.valueOf(AiGenerationTaskTypeEnum.TEXT_TO_IMAGE.getValue())
                    .equals(task.getTaskType())) {
                pictureService.getTextToImageTask(task.getTaskId(), owner);
            } else {
                pictureService.getPictureOutPaintingTask(task.getTaskId(), owner);
            }
        } catch (RuntimeException ignored) {
            // Third-party failures never make the admin list unavailable.
        }
    }

    private List<AdminAiTaskVO> toVos(List<AiGenHistory> tasks, boolean detail) {
        Set<Long> userIds = tasks.stream().map(AiGenHistory::getUserId).collect(Collectors.toSet());
        Map<Long, User> users = userIds.isEmpty() ? new HashMap<Long, User>()
                : userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        return tasks.stream().map(task -> {
            AdminAiTaskVO vo = new AdminAiTaskVO();
            vo.setId(task.getId());
            vo.setUserId(task.getUserId());
            User user = users.get(task.getUserId());
            vo.setUserName(user == null ? null : user.getUserName());
            String prompt = StrUtil.blankToDefault(task.getPrompt(), "");
            vo.setPromptSummary(StrUtil.maxLength(prompt, 80));
            if (detail) {
                vo.setPrompt(prompt);
                vo.setRequestParams(task.getRequestParams());
            }
            vo.setTaskId(task.getTaskId());
            vo.setTaskType(task.getTaskType());
            vo.setSourcePictureId(task.getSourcePictureId());
            vo.setModelName(task.getModelName());
            vo.setRequestId(task.getRequestId());
            vo.setTaskStatus(task.getTaskStatus());
            vo.setCreateTime(task.getCreateTime());
            vo.setCompletedTime(task.getCompletedTime());
            vo.setDurationMs(task.getDurationMs());
            vo.setResultCount(task.getResultCount());
            vo.setErrorCode(task.getErrorCode());
            vo.setErrorMessage(task.getErrorMessage());
            vo.setRetryFromTaskId(task.getRetryFromTaskId());
            if (StrUtil.isNotBlank(task.getImageUrl())) {
                try {
                    vo.setResultUrls(JSONUtil.toList(task.getImageUrl(), String.class));
                } catch (RuntimeException ignored) {
                    vo.setResultUrls(Collections.singletonList(task.getImageUrl()));
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private long percentile(List<Long> values, double percentile) {
        if (values.isEmpty()) return 0;
        int index = (int) Math.ceil(values.size() * percentile) - 1;
        return values.get(Math.max(0, Math.min(index, values.size() - 1)));
    }
}
