package com.mushan.tucangbackend.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PictureChangeNotifierTest {

    private PictureChangeNotifier notifier;
    private PictureCacheService cacheService;
    private PictureSearchIndexService searchIndexService;

    @BeforeEach
    void setUp() {
        notifier = new PictureChangeNotifier();
        cacheService = mock(PictureCacheService.class);
        searchIndexService = mock(PictureSearchIndexService.class);
        ReflectionTestUtils.setField(notifier, "pictureCacheService", cacheService);
        ReflectionTestUtils.setField(notifier, "pictureSearchIndexService", searchIndexService);
        ReflectionTestUtils.setField(notifier, "meterRegistry", new SimpleMeterRegistry());
    }

    @Test
    void upsertInvalidatesCacheAndTriggersSearchSync() {
        notifier.upsertAll(Arrays.asList(1L, 2L));

        verify(cacheService).invalidate();
        verify(searchIndexService).upsertAsync(Arrays.asList(1L, 2L));
    }

    @Test
    void deleteInvalidatesCacheAndRemovesSearchDocument() {
        notifier.delete(3L);

        verify(cacheService).invalidate();
        verify(searchIndexService).deleteAsync(3L);
    }
}
