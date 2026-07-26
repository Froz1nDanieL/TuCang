package com.mushan.tucangbackend.mapper;

import com.mushan.tucangbackend.model.entity.PictureAlbum;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
* @author Danie
* @description 针对表【picture_album】的数据库操作Mapper
* @createDate 2025-08-27 23:48:57
* @Entity com.mushan.tucangbackend.model.entity.PictureAlbum
*/
public interface PictureAlbumMapper extends BaseMapper<PictureAlbum> {

    @Update("UPDATE picture_album " +
            "SET pictureCount = GREATEST(COALESCE(pictureCount, 0) + #{delta}, 0) " +
            "WHERE id = #{albumId}")
    int adjustPictureCount(@Param("albumId") Long albumId, @Param("delta") int delta);
}




