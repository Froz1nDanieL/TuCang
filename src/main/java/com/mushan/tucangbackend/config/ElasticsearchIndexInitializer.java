package com.mushan.tucangbackend.config;

import com.mushan.tucangbackend.model.es.PictureEsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 确保实体注解生成的颜色字段 Mapping 在全量同步前生效。
 * 已存在且类型冲突的旧索引不会被自动删除，应通过新索引重建后切换。
 */
@Component
@ConditionalOnProperty(prefix = "tucang.elasticsearch", name = "initialize-index", havingValue = "true")
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Slf4j
public class ElasticsearchIndexInitializer implements ApplicationRunner {

    /**
     * 已有索引只允许追加本功能的新字段，不能把实体的完整 Mapping 覆盖上去。
     * Elasticsearch 不允许原地修改旧字段的 index/store 等参数。
     */
    private static final String COLOR_MAPPING = "{\"properties\":{"
            + "\"colorPalette\":{\"type\":\"nested\",\"properties\":{"
            + "\"hex\":{\"type\":\"keyword\"},"
            + "\"l\":{\"type\":\"float\"},"
            + "\"a\":{\"type\":\"float\"},"
            + "\"b\":{\"type\":\"float\"},"
            + "\"weight\":{\"type\":\"float\"}}},"
            + "\"colorTags\":{\"type\":\"keyword\"},"
            + "\"colorScores\":{\"properties\":{"
            + "\"red\":{\"type\":\"float\"},"
            + "\"orange\":{\"type\":\"float\"},"
            + "\"yellow\":{\"type\":\"float\"},"
            + "\"green\":{\"type\":\"float\"},"
            + "\"cyan\":{\"type\":\"float\"},"
            + "\"blue\":{\"type\":\"float\"},"
            + "\"purple\":{\"type\":\"float\"},"
            + "\"pink\":{\"type\":\"float\"},"
            + "\"black\":{\"type\":\"float\"},"
            + "\"white\":{\"type\":\"float\"}}},"
            + "\"colorAlgoVersion\":{\"type\":\"integer\"}"
            + "}}";

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Override
    public void run(ApplicationArguments args) {
        IndexOperations indexOperations = elasticsearchRestTemplate.indexOps(PictureEsDTO.class);
        if (!indexOperations.exists()) {
            boolean created = indexOperations.createWithMapping();
            log.info("create picture Elasticsearch index with mapping: {}", created);
            return;
        }
        boolean updated = indexOperations.putMapping(Document.parse(COLOR_MAPPING));
        log.info("update picture Elasticsearch color mapping: {}", updated);
    }
}
