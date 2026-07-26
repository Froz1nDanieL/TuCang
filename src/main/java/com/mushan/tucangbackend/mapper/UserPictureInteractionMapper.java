package com.mushan.tucangbackend.mapper;

import com.mushan.tucangbackend.model.entity.UserPictureInteraction;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
* @author mushan
* @description 针对表【user_picture_interaction(用户图片互动记录)】的数据库操作Mapper
* @createDate 2025-08-24
*/
public interface UserPictureInteractionMapper extends BaseMapper<UserPictureInteraction> {

    @Insert("INSERT IGNORE INTO user_picture_interaction " +
            "(id, userId, pictureId, type, albumId, createTime, updateTime) " +
            "VALUES (#{id}, #{userId}, #{pictureId}, 0, NULL, NOW(), NOW())")
    int insertLikeIgnore(@Param("id") Long id,
                         @Param("userId") Long userId,
                         @Param("pictureId") Long pictureId);

    @Delete("DELETE FROM user_picture_interaction " +
            "WHERE userId = #{userId} AND pictureId = #{pictureId} " +
            "AND type = 0 AND albumId IS NULL")
    int deleteLike(@Param("userId") Long userId,
                   @Param("pictureId") Long pictureId);

    @Insert("INSERT IGNORE INTO user_picture_interaction " +
            "(id, userId, pictureId, type, albumId, createTime, updateTime) " +
            "VALUES (#{id}, #{userId}, #{pictureId}, 1, #{albumId}, NOW(), NOW())")
    int insertFavoriteIgnore(@Param("id") Long id,
                             @Param("userId") Long userId,
                             @Param("pictureId") Long pictureId,
                             @Param("albumId") Long albumId);

    @Delete("DELETE FROM user_picture_interaction " +
            "WHERE userId = #{userId} AND pictureId = #{pictureId} " +
            "AND type = 1 AND albumId = #{albumId}")
    int deleteFavorite(@Param("userId") Long userId,
                       @Param("pictureId") Long pictureId,
                       @Param("albumId") Long albumId);
}
