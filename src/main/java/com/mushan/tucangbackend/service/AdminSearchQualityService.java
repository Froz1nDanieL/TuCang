package com.mushan.tucangbackend.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.mapper.AdminJobExecutionMapper;
import com.mushan.tucangbackend.mapper.PictureIndexRecordMapper;
import com.mushan.tucangbackend.model.dto.admin.AdminIndexCheckRequest;
import com.mushan.tucangbackend.model.entity.AdminJobExecution;
import com.mushan.tucangbackend.model.entity.PictureIndexRecord;
import com.mushan.tucangbackend.model.vo.admin.AdminIndexConsistencyVO;
import com.mushan.tucangbackend.model.vo.admin.AdminIndexRecordVO;
import com.mushan.tucangbackend.model.vo.admin.AdminJobExecutionVO;
import com.mushan.tucangbackend.model.vo.admin.AdminSearchQualityVO;
import com.mushan.tucangbackend.repository.PictureEsDao;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminSearchQualityService {

    @Resource private AdminJobExecutionMapper jobMapper;
    @Resource private PictureIndexRecordMapper recordMapper;
    @Resource private AdminIndexCheckRunner runner;
    @Resource private PictureSearchIndexService searchIndexService;
    @Resource private PictureEsDao pictureEsDao;

    public AdminSearchQualityVO quality() {
        AdminSearchQualityVO vo = new AdminSearchQualityVO();
        try {
            pictureEsDao.count();
            vo.setElasticsearchStatus("UP");
        } catch (RuntimeException exception) {
            vo.setElasticsearchStatus("UNAVAILABLE");
        }
        vo.setOpenMismatchCount(recordMapper.selectCount(
                new QueryWrapper<PictureIndexRecord>().eq("recordType", "CHECK")
                        .eq("resolved", 0).eq("success", 1)));
        Date since = new Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000);
        vo.setRecentSyncFailureCount(recordMapper.selectCount(
                new QueryWrapper<PictureIndexRecord>().eq("recordType", "SYNC")
                        .eq("success", 0).ge("createTime", since)));
        AdminJobExecution latest = latestJob();
        if (latest != null) {
            vo.setLastCheckTime(latest.getCompletedTime() == null
                    ? latest.getCreateTime() : latest.getCompletedTime());
            vo.setLastCheckStatus(latest.getStatus());
        }
        return vo;
    }

    public synchronized AdminJobExecutionVO start(AdminIndexCheckRequest request, Long operatorId) {
        String scope = request == null || request.getScope() == null
                ? "SAMPLE" : request.getScope().toUpperCase();
        if (!"SAMPLE".equals(scope) && !"FULL".equals(scope)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "检查范围仅支持 SAMPLE 或 FULL");
        }
        int sampleSize = request == null || request.getSampleSize() == null
                ? 100 : request.getSampleSize();
        if (sampleSize <= 0 || sampleSize > 500) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "抽样数量必须在 1 到 500 之间");
        }
        long running = jobMapper.selectCount(new QueryWrapper<AdminJobExecution>()
                .eq("jobName", "PICTURE_INDEX_CHECK").in("status", "PENDING", "RUNNING"));
        if (running > 0) {
            throw new BusinessException(ErrorCode.CONFLICT_ERROR, "已有索引检查正在执行");
        }
        AdminJobExecution job = new AdminJobExecution();
        job.setJobName("PICTURE_INDEX_CHECK");
        job.setTriggerType("MANUAL");
        job.setStatus("PENDING");
        job.setScopeType(scope);
        job.setTotalCount(0L);
        job.setProcessedCount(0L);
        job.setSuccessCount(0L);
        job.setFailureCount(0L);
        job.setOperatorId(operatorId);
        job.setIdempotencyKey("index-check:" + scope + ":" + System.currentTimeMillis());
        job.setCreateTime(new Date());
        jobMapper.insert(job);
        runner.run(job.getId(), sampleSize);
        return toJobVO(job);
    }

    public AdminIndexConsistencyVO consistency(long current, long pageSize, String mismatchType) {
        if (current <= 0 || pageSize <= 0 || pageSize > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<PictureIndexRecord> query = new QueryWrapper<>();
        query.eq("recordType", "CHECK").eq("resolved", 0)
                .like(mismatchType != null && !mismatchType.trim().isEmpty(),
                        "mismatchTypes", mismatchType)
                .orderByDesc("createTime");
        Page<PictureIndexRecord> source =
                recordMapper.selectPage(new Page<PictureIndexRecord>(current, pageSize), query);
        Page<AdminIndexRecordVO> records =
                new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        records.setRecords(source.getRecords().stream().map(this::toRecordVO).collect(Collectors.toList()));
        AdminIndexConsistencyVO vo = new AdminIndexConsistencyVO();
        vo.setLatestJob(toJobVO(latestJob()));
        vo.setRecords(records);
        return vo;
    }

    public com.mushan.tucangbackend.model.vo.admin.AdminIndexCheckVO repair(
            Long pictureId, Long operatorId) {
        return searchIndexService.repairOne(pictureId, operatorId);
    }

    private AdminJobExecution latestJob() {
        return jobMapper.selectOne(new QueryWrapper<AdminJobExecution>()
                .eq("jobName", "PICTURE_INDEX_CHECK")
                .orderByDesc("createTime").last("LIMIT 1"));
    }

    private AdminJobExecutionVO toJobVO(AdminJobExecution job) {
        if (job == null) return null;
        AdminJobExecutionVO vo = new AdminJobExecutionVO();
        BeanUtils.copyProperties(job, vo);
        return vo;
    }

    private AdminIndexRecordVO toRecordVO(PictureIndexRecord record) {
        AdminIndexRecordVO vo = new AdminIndexRecordVO();
        BeanUtils.copyProperties(record, vo, "mismatchTypes");
        if (record.getMismatchTypes() != null) {
            try {
                vo.setMismatchTypes(JSONUtil.toList(record.getMismatchTypes(), String.class));
            } catch (RuntimeException ignored) {
                vo.setMismatchTypes(Collections.singletonList(record.getMismatchTypes()));
            }
        }
        return vo;
    }
}
