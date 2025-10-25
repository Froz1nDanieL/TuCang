package com.mushan.tucangbackend.mapper;

import com.mushan.tucangbackend.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author Danie
* @description 针对表【user(用户)】的数据库操作Mapper
* @createDate 2025-07-27 14:55:17
* @Entity com.mushan.tucangbackend.model.entity.User
*/
public interface UserMapper extends BaseMapper<User> {

    /**
     * 获取最活跃用户排行（基于创建图片数量）
     *
     * @param cursorId 游标ID
     * @param limit 限制数量
     * @return 用户列表
     */
    List<User> getActiveUserRanking(@Param("cursorId") Long cursorId, @Param("limit") int limit);
    
    /**
     * 获取热门用户排行（基于创建图片的点赞数和收藏数之和）
     *
     * @param cursorId 游标ID
     * @param limit 限制数量
     * @return 用户列表
     */
    List<User> getPopularUserRanking(@Param("cursorId") Long cursorId, @Param("limit") int limit);
    
    /**
     * 获取用户创建的图片数量
     *
     * @param userId 用户ID
     * @return 图片数量
     */
    Integer getUserPictureCount(@Param("userId") Long userId);
    
    /**
     * 获取用户的热度值（点赞数和收藏数之和）
     *
     * @param userId 用户ID
     * @return 热度值
     */
    Integer getUserHeatValue(@Param("userId") Long userId);
}