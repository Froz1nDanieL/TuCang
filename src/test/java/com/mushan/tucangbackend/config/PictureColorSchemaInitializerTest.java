package com.mushan.tucangbackend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PictureColorSchemaInitializerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ApplicationArguments applicationArguments;

    @InjectMocks
    private PictureColorSchemaInitializer initializer;

    @Test
    void shouldKeepExistingPicColorAndAddOnlyMissingColumns() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(Arrays.asList("id", "picColor"));

        initializer.run(applicationArguments);

        ArgumentCaptor<String> ddlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(4)).execute(ddlCaptor.capture());
        List<String> statements = ddlCaptor.getAllValues();
        assertTrue(statements.stream().anyMatch(sql -> sql.contains("ADD COLUMN colorPalette")));
        assertTrue(statements.stream().anyMatch(sql -> sql.contains("ADD COLUMN colorTags")));
        assertTrue(statements.stream().anyMatch(sql -> sql.contains("ADD COLUMN colorScores")));
        assertTrue(statements.stream().anyMatch(sql -> sql.contains("ADD COLUMN colorAlgoVersion")));
        assertFalse(statements.stream().anyMatch(sql -> sql.contains("ADD COLUMN picColor")));
    }
}
