package com.mushan.tucangbackend.repository;

import com.mushan.tucangbackend.model.es.PictureEsDTO;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 图片 Elasticsearch 操作接口
 */
public interface PictureEsDao extends ElasticsearchRepository<PictureEsDTO, Long> {
    

}