package com.mushan.tucangbackend.manager.Job;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushan.tucangbackend.config.CosClientConfig;
import com.mushan.tucangbackend.model.color.ColorAnalysisResult;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.es.PictureEsDTO;
import com.mushan.tucangbackend.repository.PictureEsDao;
import com.mushan.tucangbackend.service.PictureService;
import com.mushan.tucangbackend.utils.ColorPaletteUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.net.URI;
import java.util.List;

/**
 * 历史图片颜色回填任务。
 *
 * 默认关闭；完成数据库迁移和 ES Mapping 更新后，通过
 * PICTURE_COLOR_SEARCH_BACKFILL_ENABLED=true 启用一次。
 */
@Component
@ConditionalOnProperty(prefix = "picture.color-search", name = "backfill-enabled", havingValue = "true")
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class PictureColorBackfillJob implements ApplicationRunner {

    private static final int PAGE_SIZE = 100;

    @Resource
    private PictureService pictureService;

    @Resource
    private PictureEsDao pictureEsDao;

    @Resource
    private CosClientConfig cosClientConfig;

    @Override
    public void run(ApplicationArguments args) {
        long cursorId = 0L;
        int successCount = 0;
        int failedCount = 0;
        while (true) {
            Page<Picture> page = pictureService.lambdaQuery()
                    .gt(Picture::getId, cursorId)
                    .and(wrapper -> wrapper.isNull(Picture::getColorAlgoVersion)
                            .or()
                            .lt(Picture::getColorAlgoVersion, ColorPaletteUtils.ALGORITHM_VERSION))
                    .orderByAsc(Picture::getId)
                    .page(new Page<>(1, PAGE_SIZE, false));
            List<Picture> pictures = page.getRecords();
            if (pictures.isEmpty()) {
                break;
            }
            for (Picture picture : pictures) {
                cursorId = picture.getId();
                try {
                    ColorAnalysisResult analysisResult = analyzePicture(picture);
                    applyAnalysis(picture, analysisResult);
                    boolean updated = pictureService.lambdaUpdate()
                            .eq(Picture::getId, picture.getId())
                            .set(Picture::getPicColor, picture.getPicColor())
                            .set(Picture::getColorPalette, picture.getColorPalette())
                            .set(Picture::getColorTags, picture.getColorTags())
                            .set(Picture::getColorScores, picture.getColorScores())
                            .set(Picture::getColorAlgoVersion, picture.getColorAlgoVersion())
                            .update();
                    if (!updated) {
                        throw new IllegalStateException("数据库颜色字段更新失败");
                    }
                    pictureEsDao.save(PictureEsDTO.objToDto(picture));
                    successCount++;
                } catch (Exception e) {
                    failedCount++;
                    log.warn("backfill picture color failed, pictureId: {}", picture.getId(), e);
                }
            }
            log.info("picture color backfill progress, cursorId: {}, success: {}, failed: {}",
                    cursorId, successCount, failedCount);
        }
        log.info("picture color backfill finished, success: {}, failed: {}", successCount, failedCount);
    }

    private ColorAnalysisResult analyzePicture(Picture picture) throws Exception {
        if (isTrustedPictureUrl(picture.getUrl())) {
            File tempFile = File.createTempFile("picture_color_", ".img");
            try {
                try (HttpResponse response = HttpRequest.get(picture.getUrl())
                        .timeout(15_000)
                        .execute()) {
                    if (!response.isOk()) {
                        throw new IllegalStateException("下载图片失败，HTTP " + response.getStatus());
                    }
                    response.writeBody(tempFile);
                }
                return ColorPaletteUtils.analyze(tempFile);
            } finally {
                FileUtil.del(tempFile);
            }
        }
        if (StrUtil.isNotBlank(picture.getPicColor())) {
            return ColorPaletteUtils.fromAverageColor(picture.getPicColor());
        }
        throw new IllegalArgumentException("图片地址不可信且没有可用平均色");
    }

    private boolean isTrustedPictureUrl(String pictureUrl) {
        try {
            URI pictureUri = URI.create(pictureUrl);
            URI cosUri = URI.create(cosClientConfig.getHost());
            String scheme = pictureUri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && pictureUri.getHost() != null
                    && pictureUri.getHost().equalsIgnoreCase(cosUri.getHost());
        } catch (Exception e) {
            return false;
        }
    }

    private void applyAnalysis(Picture picture, ColorAnalysisResult analysisResult) {
        picture.setPicColor(analysisResult.getDominantColor());
        picture.setColorPalette(JSONUtil.toJsonStr(analysisResult.getPalette()));
        picture.setColorTags(JSONUtil.toJsonStr(analysisResult.getColorTags()));
        picture.setColorScores(JSONUtil.toJsonStr(analysisResult.getColorScores()));
        picture.setColorAlgoVersion(analysisResult.getAlgorithmVersion());
    }
}
