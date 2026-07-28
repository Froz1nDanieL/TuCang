package com.mushan.tucangbackend.service;

import com.mushan.tucangbackend.mapper.PictureMapper;
import com.mushan.tucangbackend.mapper.PictureIndexRecordMapper;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.entity.PictureIndexRecord;
import com.mushan.tucangbackend.model.es.PictureEsDTO;
import com.mushan.tucangbackend.model.vo.admin.AdminIndexCheckVO;
import com.mushan.tucangbackend.repository.PictureEsDao;
import cn.hutool.json.JSONUtil;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Date;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PictureSearchIndexService {

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private PictureEsDao pictureEsDao;

    @Resource
    private MeterRegistry meterRegistry;

    @Resource
    private PictureIndexRecordMapper pictureIndexRecordMapper;

    @Async
    public void upsertAsync(List<Long> pictureIds) {
        if (pictureIds == null || pictureIds.isEmpty()) {
            return;
        }
        try {
            List<Picture> pictures = pictureMapper.selectBatchIds(pictureIds);
            if (!pictures.isEmpty()) {
                for (Picture picture : pictures) {
                    upsertNow(picture.getId(), "IMMEDIATE", null);
                }
            }
            meterRegistry.counter(
                    "tucang.es.sync", "type", "immediate", "operation", "upsert", "result", "success"
            ).increment();
        } catch (RuntimeException exception) {
            meterRegistry.counter(
                    "tucang.es.sync", "type", "immediate", "operation", "upsert", "result", "failed"
            ).increment();
            log.error("Failed to update picture search index, ids={}", pictureIds, exception);
        }
    }

    @Async
    public void deleteAsync(Long pictureId) {
        if (pictureId == null) {
            return;
        }
        try {
            deleteNow(pictureId, "IMMEDIATE", null);
            meterRegistry.counter(
                    "tucang.es.sync", "type", "immediate", "operation", "delete", "result", "success"
            ).increment();
        } catch (RuntimeException exception) {
            meterRegistry.counter(
                    "tucang.es.sync", "type", "immediate", "operation", "delete", "result", "failed"
            ).increment();
            log.error("Failed to delete picture search index, id={}", pictureId, exception);
        }
    }

    public void upsertNow(Long pictureId, String syncType, Long operatorId) {
        long started = System.currentTimeMillis();
        try {
            Picture picture = pictureMapper.selectById(pictureId);
            if (picture == null) {
                deleteNow(pictureId, syncType, operatorId);
                return;
            }
            pictureEsDao.save(PictureEsDTO.objToDto(picture));
            recordSync(pictureId, syncType, "UPSERT", true, null,
                    System.currentTimeMillis() - started, operatorId);
        } catch (RuntimeException exception) {
            recordSync(pictureId, syncType, "UPSERT", false, safeMessage(exception),
                    System.currentTimeMillis() - started, operatorId);
            throw exception;
        }
    }

    public void deleteNow(Long pictureId, String syncType, Long operatorId) {
        long started = System.currentTimeMillis();
        try {
            pictureEsDao.deleteById(pictureId);
            recordSync(pictureId, syncType, "DELETE", true, null,
                    System.currentTimeMillis() - started, operatorId);
        } catch (RuntimeException exception) {
            recordSync(pictureId, syncType, "DELETE", false, safeMessage(exception),
                    System.currentTimeMillis() - started, operatorId);
            throw exception;
        }
    }

    public AdminIndexCheckVO checkOne(Long pictureId, String batchId, Long operatorId) {
        long started = System.currentTimeMillis();
        AdminIndexCheckVO result = new AdminIndexCheckVO();
        result.setPictureId(pictureId);
        try {
            Picture mysql = pictureMapper.selectById(pictureId);
            Optional<PictureEsDTO> esOptional = pictureEsDao.findById(pictureId);
            PictureEsDTO es = esOptional.orElse(null);
            result.setMysqlExists(mysql != null);
            result.setEsExists(es != null);
            List<String> mismatches = result.getMismatchTypes();
            if (mysql != null && es == null) {
                mismatches.add("MYSQL_ONLY");
            } else if (mysql == null && es != null) {
                mismatches.add("ES_ORPHAN");
            } else if (mysql != null) {
                if (!equalsValue(mysql.getReviewStatus(), es.getReviewStatus())) {
                    mismatches.add("REVIEW_STATUS");
                }
                if (!dateClose(mysql.getUpdateTime(), es.getUpdateTime())) {
                    mismatches.add("UPDATE_TIME");
                }
                if (!equalsValue(mysql.getColorAlgoVersion(), es.getColorAlgoVersion())) {
                    mismatches.add("COLOR_VERSION");
                }
            }
            result.setStatus("UP");
            persistCheck(result, batchId, operatorId, System.currentTimeMillis() - started);
            return result;
        } catch (RuntimeException exception) {
            result.setStatus("UNAVAILABLE");
            result.setMessage(safeMessage(exception));
            persistCheck(result, batchId, operatorId, System.currentTimeMillis() - started);
            return result;
        }
    }

    public AdminIndexCheckVO repairOne(Long pictureId, Long operatorId) {
        AdminIndexCheckVO before = checkOne(pictureId, null, operatorId);
        if (!"UP".equals(before.getStatus())) {
            return before;
        }
        if (before.getMismatchTypes().contains("ES_ORPHAN")) {
            deleteNow(pictureId, "MANUAL", operatorId);
        } else if (!before.getMismatchTypes().isEmpty()) {
            upsertNow(pictureId, "MANUAL", operatorId);
        }
        AdminIndexCheckVO after = checkOne(pictureId, null, operatorId);
        if (after.isConsistent()) {
            com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<PictureIndexRecord> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
            wrapper.eq("pictureId", pictureId)
                    .eq("recordType", "CHECK")
                    .eq("resolved", 0)
                    .set("resolved", 1)
                    .set("resolvedTime", new Date());
            pictureIndexRecordMapper.update(null, wrapper);
        }
        return after;
    }

    private void persistCheck(AdminIndexCheckVO result, String batchId, Long operatorId, long durationMs) {
        PictureIndexRecord record = new PictureIndexRecord();
        record.setPictureId(result.getPictureId());
        record.setRecordType("CHECK");
        record.setBatchId(batchId);
        record.setOperation("CHECK");
        record.setMismatchTypes(JSONUtil.toJsonStr(result.getMismatchTypes()));
        record.setSuccess("UP".equals(result.getStatus()) ? 1 : 0);
        record.setErrorMessage(result.getMessage());
        record.setDurationMs(durationMs);
        record.setResolved(result.getMismatchTypes().isEmpty() ? 1 : 0);
        record.setResolvedTime(result.getMismatchTypes().isEmpty() ? new Date() : null);
        record.setOperatorId(operatorId);
        record.setCreateTime(new Date());
        pictureIndexRecordMapper.insert(record);
    }

    private void recordSync(Long pictureId, String syncType, String operation, boolean success,
                            String error, long durationMs, Long operatorId) {
        PictureIndexRecord record = new PictureIndexRecord();
        record.setPictureId(pictureId);
        record.setRecordType("SYNC");
        record.setSyncType(syncType);
        record.setOperation(operation);
        record.setSuccess(success ? 1 : 0);
        record.setErrorMessage(error);
        record.setDurationMs(durationMs);
        record.setResolved(success ? 1 : 0);
        record.setResolvedTime(success ? new Date() : null);
        record.setOperatorId(operatorId);
        record.setCreateTime(new Date());
        pictureIndexRecordMapper.insert(record);
    }

    private boolean dateClose(Date left, Date right) {
        if (left == null || right == null) {
            return left == right;
        }
        return Math.abs(left.getTime() - right.getTime()) <= 1000L;
    }

    private boolean equalsValue(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null) {
            return throwable.getClass().getSimpleName();
        }
        return message.length() > 512 ? message.substring(0, 512) : message;
    }
}
