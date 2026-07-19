package com.mushan.tucangbackend.mapper;

import com.mushan.tucangbackend.model.entity.Picture;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
* @author Danie
* @description 针对表【picture(图片)】的数据库操作Mapper
* @createDate 2025-07-29 15:19:40
* @Entity generator.domain.Picture
*/
public interface PictureMapper extends BaseMapper<Picture> {

    /**
     * 查询所有图片，包括已经被删除的图片
     *
     * @param minUpdateTime 最小更新时间，只查询该时间之后更新的图片
     * @return 图片列表
     */
    @Select("SELECT * FROM picture WHERE updateTime >= #{minUpdateTime}")
    List<Picture> listPictureWithDelete(Date minUpdateTime);
}