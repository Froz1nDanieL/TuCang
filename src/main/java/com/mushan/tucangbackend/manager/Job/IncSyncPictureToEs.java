package com.mushan.tucangbackend.manager.Job;

import cn.hutool.core.collection.CollUtil;
import com.mushan.tucangbackend.mapper.PictureMapper;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.es.PictureEsDTO;
import com.mushan.tucangbackend.repository.PictureEsDao;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

// todo 取消注释开启任务
@Component
@ConditionalOnProperty(prefix = "tucang.elasticsearch.sync", name = "incremental-enabled", havingValue = "true")
@Slf4j
public class IncSyncPictureToEs {

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private PictureEsDao pictureEsDao;

    @Resource
    private MeterRegistry meterRegistry;

    /**
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60 * 1000)
    public void run() {
        // 查询近 5 分钟内的数据
        long FIVE_MINUTES = 5 * 60 * 1000L;
        Date fiveMinutesAgoDate = new Date(new Date().getTime() - FIVE_MINUTES);
        try {
            List<Picture> pictureList = pictureMapper.listPictureWithDelete(fiveMinutesAgoDate);
            if (CollUtil.isEmpty(pictureList)) {
                log.info("no inc picture");
                return;
            }
            List<PictureEsDTO> pictureEsDTOList = pictureList.stream()
                    .map(PictureEsDTO::objToDto)
                    .collect(Collectors.toList());
            final int pageSize = 500;
            int total = pictureEsDTOList.size();
            log.info("IncSyncPictureToEs start, total {}", total);
            for (int i = 0; i < total; i += pageSize) {
                int end = Math.min(i + pageSize, total);
                log.info("sync from {} to {}", i, end);
                pictureEsDao.saveAll(pictureEsDTOList.subList(i, end));
            }
            meterRegistry.counter("tucang.es.sync", "type", "incremental", "result", "success").increment();
            log.info("IncSyncPictureToEs end, total {}", total);
        } catch (RuntimeException exception) {
            meterRegistry.counter("tucang.es.sync", "type", "incremental", "result", "failed").increment();
            throw exception;
        }
    }
}
