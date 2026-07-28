package com.mushan.tucangbackend.service;

import com.mushan.tucangbackend.mapper.PictureMapper;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.es.PictureEsDTO;
import com.mushan.tucangbackend.repository.PictureEsDao;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
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

    @Async
    public void upsertAsync(List<Long> pictureIds) {
        if (pictureIds == null || pictureIds.isEmpty()) {
            return;
        }
        try {
            List<Picture> pictures = pictureMapper.selectBatchIds(pictureIds);
            if (!pictures.isEmpty()) {
                pictureEsDao.saveAll(pictures.stream()
                        .map(PictureEsDTO::objToDto)
                        .collect(Collectors.toList()));
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
            pictureEsDao.deleteById(pictureId);
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
}
