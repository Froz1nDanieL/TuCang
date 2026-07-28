package com.mushan.tucangbackend.mapper;

import com.mushan.tucangbackend.model.entity.Picture;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

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

    @Update("UPDATE picture SET likeCount = GREATEST(COALESCE(likeCount, 0) + #{delta}, 0) " +
            "WHERE id = #{pictureId} AND isDelete = 0")
    int adjustLikeCount(@Param("pictureId") Long pictureId, @Param("delta") int delta);

    @Update("UPDATE picture SET favoriteCount = GREATEST(COALESCE(favoriteCount, 0) + #{delta}, 0) " +
            "WHERE id = #{pictureId} AND isDelete = 0")
    int adjustFavoriteCount(@Param("pictureId") Long pictureId, @Param("delta") int delta);

    @Select("SELECT id FROM picture ORDER BY id")
    List<Long> listAllPictureIdsWithDelete();

    @Select("SELECT id FROM picture ORDER BY updateTime DESC, id DESC LIMIT #{limit}")
    List<Long> listRecentPictureIdsWithDelete(@Param("limit") int limit);

    @Select("SELECT id FROM picture WHERE id > #{afterId} ORDER BY id LIMIT #{limit}")
    List<Long> listPictureIdsWithDeleteAfter(@Param("afterId") long afterId,
                                             @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM picture")
    long countAllPicturesWithDelete();
}
