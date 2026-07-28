package com.mushan.tucangbackend.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class PictureChangeNotifier {

    @Resource
    private PictureCacheService pictureCacheService;

    @Resource
    private PictureSearchIndexService pictureSearchIndexService;

    @Resource
    private MeterRegistry meterRegistry;

    public void upsert(Long pictureId) {
        upsertAll(Collections.singletonList(pictureId));
    }

    public void upsertAll(List<Long> pictureIds) {
        if (pictureIds == null || pictureIds.isEmpty()) {
            return;
        }
        afterCommit(() -> {
            invalidateCache();
            pictureSearchIndexService.upsertAsync(pictureIds);
        });
    }

    public void delete(Long pictureId) {
        if (pictureId == null) {
            return;
        }
        afterCommit(() -> {
            invalidateCache();
            pictureSearchIndexService.deleteAsync(pictureId);
        });
    }

    private void invalidateCache() {
        try {
            pictureCacheService.invalidate();
            meterRegistry.counter("tucang.picture.cache.invalidation", "result", "success").increment();
        } catch (RuntimeException exception) {
            meterRegistry.counter("tucang.picture.cache.invalidation", "result", "failed").increment();
            log.error("Failed to invalidate picture cache version", exception);
        }
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
