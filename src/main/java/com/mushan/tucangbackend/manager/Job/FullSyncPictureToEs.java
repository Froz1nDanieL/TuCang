package com.mushan.tucangbackend.manager.Job;

import cn.hutool.core.collection.CollUtil;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.es.PictureEsDTO;
import com.mushan.tucangbackend.repository.PictureEsDao;
import com.mushan.tucangbackend.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@Slf4j
public class FullSyncPictureToEs implements CommandLineRunner {

    @Resource
    private PictureService pictureService;

    @Resource
    private PictureEsDao pictureEsDao;

    @Override
    public void run(String... args) {
        // 全量获取题目（数据量不大的情况下使用）
        List<Picture> pictureList = pictureService.list();
        if (CollUtil.isEmpty(pictureList)) {
            return;
        }
        // 转为 ES 实体类
        List<PictureEsDTO> pictureEsDTOList = pictureList.stream()
                .map(PictureEsDTO::objToDto)
                .collect(Collectors.toList());
        // 分页批量插入到 ES
        final int pageSize = 500;
        int total = pictureEsDTOList.size();
        log.info("FullSyncPictureToEs start, total {}", total);
        for (int i = 0; i < total; i += pageSize) {
            // 注意同步的数据下标不能超过总数据量
            int end = Math.min(i + pageSize, total);
            log.info("sync from {} to {}", i, end);
            pictureEsDao.saveAll(pictureEsDTOList.subList(i, end));
        }
        log.info("FullSyncPictureToEs end, total {}", total);
    }
}
