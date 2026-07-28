package com.mushan.tucangbackend.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mushan.tucangbackend.mapper.AdminJobExecutionMapper;
import com.mushan.tucangbackend.mapper.PictureMapper;
import com.mushan.tucangbackend.model.entity.AdminJobExecution;
import com.mushan.tucangbackend.model.es.PictureEsDTO;
import com.mushan.tucangbackend.model.vo.admin.AdminIndexCheckVO;
import com.mushan.tucangbackend.repository.PictureEsDao;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminIndexCheckRunner {

    @Resource private AdminJobExecutionMapper jobMapper;
    @Resource private PictureMapper pictureMapper;
    @Resource private PictureEsDao pictureEsDao;
    @Resource private PictureSearchIndexService searchIndexService;
    @Resource private MeterRegistry meterRegistry;

    @Value("${tucang.admin.index-check-page-size:200}")
    private int configuredPageSize;

    @Async
    public void run(Long jobId, int sampleSize) {
        AdminJobExecution job = jobMapper.selectById(jobId);
        if (job == null) return;
        update(jobId, "RUNNING", 0L, 0L, 0L, null, new Date(), null);
        try {
            if ("SAMPLE".equals(job.getScopeType())) {
                runSample(job, sampleSize);
            } else {
                runFull(job);
            }
            meterRegistry.counter("tucang.es.consistency.check", "result", "success").increment();
        } catch (RuntimeException exception) {
            update(jobId, "FAILED", null, null, null,
                    safe(exception), null, new Date());
            meterRegistry.counter("tucang.es.consistency.check", "result", "failed").increment();
        }
    }

    private void runSample(AdminJobExecution job, int sampleSize) {
        Set<Long> ids = new LinkedHashSet<>();
        ids.addAll(pictureMapper.listRecentPictureIdsWithDelete(sampleSize));
        org.springframework.data.domain.Page<PictureEsDTO> esPage = pictureEsDao.findAll(
                PageRequest.of(0, sampleSize, Sort.by(Sort.Direction.DESC, "updateTime")));
        for (PictureEsDTO es : esPage.getContent()) {
            ids.add(es.getId());
        }
        updateTotals(job.getId(), ids.size());
        long[] progress = new long[3];
        for (Long id : ids) {
            check(job, id, progress);
        }
        update(job.getId(), "SUCCEEDED", progress[0], progress[1], progress[2],
                null, null, new Date());
    }

    private void runFull(AdminJobExecution job) {
        int pageSize = Math.max(10, Math.min(configuredPageSize, 1000));
        long upperBound = pictureMapper.countAllPicturesWithDelete() + pictureEsDao.count();
        updateTotals(job.getId(), upperBound);
        Set<Long> checkedIds = new HashSet<>();
        long[] progress = new long[3];

        long afterId = 0L;
        while (true) {
            List<Long> ids = pictureMapper.listPictureIdsWithDeleteAfter(afterId, pageSize);
            if (ids.isEmpty()) break;
            for (Long id : ids) {
                if (checkedIds.add(id)) check(job, id, progress);
            }
            afterId = ids.get(ids.size() - 1);
            if (ids.size() < pageSize) break;
        }

        int pageNumber = 0;
        while (true) {
            org.springframework.data.domain.Page<PictureEsDTO> page = pictureEsDao.findAll(
                    PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.ASC, "id")));
            for (PictureEsDTO es : page.getContent()) {
                if (checkedIds.add(es.getId())) check(job, es.getId(), progress);
            }
            if (!page.hasNext()) break;
            pageNumber++;
        }
        updateTotals(job.getId(), progress[0]);
        update(job.getId(), "SUCCEEDED", progress[0], progress[1], progress[2],
                null, null, new Date());
    }

    private void check(AdminJobExecution job, Long pictureId, long[] progress) {
        AdminIndexCheckVO result = searchIndexService.checkOne(
                pictureId, String.valueOf(job.getId()), job.getOperatorId());
        progress[0]++;
        if (result.isConsistent()) progress[1]++; else progress[2]++;
        if (progress[0] % 50 == 0) {
            updateProgress(job.getId(), progress[0], progress[1], progress[2]);
        }
    }

    private void updateTotals(Long id, long total) {
        jobMapper.update(null, new UpdateWrapper<AdminJobExecution>()
                .eq("id", id).set("totalCount", total));
    }

    private void updateProgress(Long id, long processed, long success, long failures) {
        jobMapper.update(null, new UpdateWrapper<AdminJobExecution>().eq("id", id)
                .set("processedCount", processed).set("successCount", success)
                .set("failureCount", failures));
    }

    private void update(Long id, String status, Long processed, Long success, Long failures,
                        String error, Date started, Date completed) {
        UpdateWrapper<AdminJobExecution> update = new UpdateWrapper<>();
        update.eq("id", id).set("status", status);
        if (processed != null) update.set("processedCount", processed);
        if (success != null) update.set("successCount", success);
        if (failures != null) update.set("failureCount", failures);
        if (error != null) update.set("errorMessage", error);
        if (started != null) update.set("startedTime", started);
        if (completed != null) update.set("completedTime", completed);
        jobMapper.update(null, update);
    }

    private String safe(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null) return throwable.getClass().getSimpleName();
        return message.substring(0, Math.min(message.length(), 512));
    }
}
