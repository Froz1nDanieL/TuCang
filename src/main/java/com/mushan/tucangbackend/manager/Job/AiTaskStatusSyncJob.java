package com.mushan.tucangbackend.manager.Job;

import com.mushan.tucangbackend.constant.AiTaskStatusConstant;
import com.mushan.tucangbackend.model.entity.AiGenHistory;
import com.mushan.tucangbackend.service.AdminAiTaskService;
import com.mushan.tucangbackend.service.AiGenHistoryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "tucang.ai", name = "task-sync-enabled", havingValue = "true")
public class AiTaskStatusSyncJob {

    @Resource private AiGenHistoryService aiGenHistoryService;
    @Resource private AdminAiTaskService adminAiTaskService;

    @Scheduled(fixedDelayString = "${tucang.ai.task-sync-delay-ms:30000}")
    public void sync() {
        List<AiGenHistory> tasks = aiGenHistoryService.lambdaQuery()
                .in(AiGenHistory::getTaskStatus, Arrays.asList(
                        AiTaskStatusConstant.PENDING,
                        AiTaskStatusConstant.RUNNING,
                        AiTaskStatusConstant.UNKNOWN))
                .orderByAsc(AiGenHistory::getCreateTime)
                .last("LIMIT 100")
                .list();
        for (AiGenHistory task : tasks) {
            adminAiTaskService.refreshQuietly(task);
        }
    }
}
