package com.mushan.tucangbackend.service;

import com.mushan.tucangbackend.mapper.PictureIndexRecordMapper;
import com.mushan.tucangbackend.mapper.PictureMapper;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.entity.PictureIndexRecord;
import com.mushan.tucangbackend.model.es.PictureEsDTO;
import com.mushan.tucangbackend.model.vo.admin.AdminIndexCheckVO;
import com.mushan.tucangbackend.repository.PictureEsDao;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PictureSearchIndexServiceTest {

    private PictureSearchIndexService service;
    private PictureMapper pictureMapper;
    private PictureEsDao pictureEsDao;
    private PictureIndexRecordMapper recordMapper;

    @BeforeEach
    void setUp() {
        service = new PictureSearchIndexService();
        pictureMapper = mock(PictureMapper.class);
        pictureEsDao = mock(PictureEsDao.class);
        recordMapper = mock(PictureIndexRecordMapper.class);
        ReflectionTestUtils.setField(service, "pictureMapper", pictureMapper);
        ReflectionTestUtils.setField(service, "pictureEsDao", pictureEsDao);
        ReflectionTestUtils.setField(service, "pictureIndexRecordMapper", recordMapper);
        ReflectionTestUtils.setField(service, "meterRegistry", new SimpleMeterRegistry());
    }

    @Test
    void detectsMysqlOnlyAndPersistsOpenMismatch() {
        Picture mysql = picture(1L, 1, 1, new Date());
        when(pictureMapper.selectById(1L)).thenReturn(mysql);
        when(pictureEsDao.findById(1L)).thenReturn(Optional.empty());

        AdminIndexCheckVO result = service.checkOne(1L, "batch-1", 9L);

        assertEquals("UP", result.getStatus());
        assertEquals(java.util.Collections.singletonList("MYSQL_ONLY"), result.getMismatchTypes());
        ArgumentCaptor<PictureIndexRecord> captor = ArgumentCaptor.forClass(PictureIndexRecord.class);
        verify(recordMapper).insert(captor.capture());
        assertEquals(Integer.valueOf(0), captor.getValue().getResolved());
        assertEquals("batch-1", captor.getValue().getBatchId());
    }

    @Test
    void detectsEsOrphan() {
        PictureEsDTO es = es(2L, 1, 1, new Date());
        when(pictureMapper.selectById(2L)).thenReturn(null);
        when(pictureEsDao.findById(2L)).thenReturn(Optional.of(es));

        AdminIndexCheckVO result = service.checkOne(2L, null, null);

        assertEquals(java.util.Collections.singletonList("ES_ORPHAN"), result.getMismatchTypes());
    }

    @Test
    void detectsReviewTimeAndColorDifferencesTogether() {
        Picture mysql = picture(3L, 1, 2, new Date(10_000L));
        PictureEsDTO es = es(3L, 0, 1, new Date(50_000L));
        when(pictureMapper.selectById(3L)).thenReturn(mysql);
        when(pictureEsDao.findById(3L)).thenReturn(Optional.of(es));

        AdminIndexCheckVO result = service.checkOne(3L, "batch-3", 9L);

        assertTrue(result.getMismatchTypes().contains("REVIEW_STATUS"));
        assertTrue(result.getMismatchTypes().contains("UPDATE_TIME"));
        assertTrue(result.getMismatchTypes().contains("COLOR_VERSION"));
        assertEquals(3, result.getMismatchTypes().size());
    }

    @Test
    void degradesToUnavailableWhenElasticsearchFails() {
        when(pictureMapper.selectById(4L)).thenReturn(picture(4L, 1, 1, new Date()));
        when(pictureEsDao.findById(4L)).thenThrow(new IllegalStateException("es offline"));

        AdminIndexCheckVO result = service.checkOne(4L, null, null);

        assertEquals("UNAVAILABLE", result.getStatus());
        assertEquals("es offline", result.getMessage());
        verify(recordMapper).insert(any(PictureIndexRecord.class));
    }

    private Picture picture(Long id, int reviewStatus, int colorVersion, Date updateTime) {
        Picture picture = new Picture();
        picture.setId(id);
        picture.setReviewStatus(reviewStatus);
        picture.setColorAlgoVersion(colorVersion);
        picture.setUpdateTime(updateTime);
        return picture;
    }

    private PictureEsDTO es(Long id, int reviewStatus, int colorVersion, Date updateTime) {
        PictureEsDTO es = new PictureEsDTO();
        es.setId(id);
        es.setReviewStatus(reviewStatus);
        es.setColorAlgoVersion(colorVersion);
        es.setUpdateTime(updateTime);
        return es;
    }
}
