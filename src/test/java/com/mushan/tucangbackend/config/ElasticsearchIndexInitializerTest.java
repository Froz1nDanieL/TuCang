package com.mushan.tucangbackend.config;

import com.mushan.tucangbackend.model.es.PictureEsDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElasticsearchIndexInitializerTest {

    @Mock
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Mock
    private IndexOperations indexOperations;

    @Mock
    private ApplicationArguments applicationArguments;

    @InjectMocks
    private ElasticsearchIndexInitializer initializer;

    @Test
    void shouldOnlyAppendColorFieldsToExistingIndex() {
        when(elasticsearchRestTemplate.indexOps(eq(PictureEsDTO.class))).thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(true);
        when(indexOperations.putMapping(org.mockito.ArgumentMatchers.any(Document.class))).thenReturn(true);

        initializer.run(applicationArguments);

        ArgumentCaptor<Document> mappingCaptor = ArgumentCaptor.forClass(Document.class);
        verify(indexOperations).putMapping(mappingCaptor.capture());
        String mappingJson = mappingCaptor.getValue().toJson();
        assertTrue(mappingJson.contains("colorPalette"));
        assertTrue(mappingJson.contains("colorTags"));
        assertTrue(mappingJson.contains("colorScores"));
        assertFalse(mappingJson.contains("reviewTime"));
        assertFalse(mappingJson.contains("createTime"));
    }
}
