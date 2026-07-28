package com.mushan.tucangbackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 兼容已经存在 picColor、但尚未增加 Lab 调色板字段的历史数据库。
 * 每个字段独立检查和创建，避免一个重复字段导致整条 ALTER TABLE 回滚。
 */
@Component
@ConditionalOnProperty(prefix = "tucang.database", name = "initialize-picture-color-schema", havingValue = "true")
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class PictureColorSchemaInitializer implements ApplicationRunner {

    private static final String QUERY_COLUMNS_SQL =
            "SELECT COLUMN_NAME FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'picture'";

    private static final List<ColumnDefinition> COLOR_COLUMNS = Arrays.asList(
            new ColumnDefinition(
                    "picColor",
                    "ALTER TABLE picture ADD COLUMN picColor varchar(16) NULL COMMENT '图片主色调' AFTER favoriteCount"
            ),
            new ColumnDefinition(
                    "colorPalette",
                    "ALTER TABLE picture ADD COLUMN colorPalette json NULL COMMENT 'Lab 调色板 JSON' AFTER picColor"
            ),
            new ColumnDefinition(
                    "colorTags",
                    "ALTER TABLE picture ADD COLUMN colorTags json NULL COMMENT '十种标准色标签 JSON' AFTER colorPalette"
            ),
            new ColumnDefinition(
                    "colorScores",
                    "ALTER TABLE picture ADD COLUMN colorScores json NULL COMMENT '十种标准色离线分数 JSON' AFTER colorTags"
            ),
            new ColumnDefinition(
                    "colorAlgoVersion",
                    "ALTER TABLE picture ADD COLUMN colorAlgoVersion int DEFAULT 1 NULL COMMENT '颜色算法版本' AFTER colorScores"
            )
    );

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<String> existingColumnNames = jdbcTemplate.queryForList(QUERY_COLUMNS_SQL, String.class);
        if (existingColumnNames.isEmpty()) {
            throw new IllegalStateException("数据库中不存在 picture 表，请先执行 create.sql");
        }
        Set<String> existingColumns = new HashSet<>();
        for (String columnName : existingColumnNames) {
            existingColumns.add(columnName.toLowerCase(Locale.ROOT));
        }
        for (ColumnDefinition column : COLOR_COLUMNS) {
            if (existingColumns.contains(column.name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            jdbcTemplate.execute(column.ddl);
            existingColumns.add(column.name.toLowerCase(Locale.ROOT));
            log.info("added picture color-search column: {}", column.name);
        }
    }

    private static final class ColumnDefinition {
        private final String name;
        private final String ddl;

        private ColumnDefinition(String name, String ddl) {
            this.name = name;
            this.ddl = ddl;
        }
    }
}
