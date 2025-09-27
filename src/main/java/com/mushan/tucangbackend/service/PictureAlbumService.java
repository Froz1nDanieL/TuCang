package com.mushan.tucangbackend.service;

import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.entity.PictureAlbum;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mushan.tucangbackend.model.entity.User;

import java.util.List;

/**
* @author Danie
* @description 针对表【picture_album】的数据库操作Service
* @createDate 2025-08-27 23:48:57
*/
public interface PictureAlbumService extends IService<PictureAlbum> {

    /**
     * 增加收藏夹浏览数
     *
     * @param albumId 收藏夹ID
     * @return boolean 是否更新成功
     */
    boolean increaseViewCount(Long albumId);

    /**
     * 获取精选收藏夹列表（所有用户看到的内容相同）
     *
     * @param limit 返回数量
     * @return 精选收藏夹列表
     */
    List<PictureAlbum> getFeaturedAlbums(int limit);
    
    /**
     * 保存每日精选收藏夹
     * 
     * @param featuredAlbums 精选收藏夹列表
     * @return boolean 是否保存成功
     */
    boolean saveDailyFeaturedAlbums(List<PictureAlbum> featuredAlbums);

    List<PictureAlbum> getTodayFeaturedAlbumsFromCache(int limit);

    /**
     * 根据分类获取热门图片
     * 
     * @param category 分类
     * @param limit 限制数量
     * @return 热门图片列表
     */
    List<PictureAlbum> getHotPicturesByCategory(String category, int limit);
    
    /**
     * 创建或更新系统热门收藏夹
     * 
     * @param category 分类
     * @param albumName 收藏夹名称
     * @param systemUserId 系统用户ID
     * @return PictureAlbum 收藏夹对象
     */
    PictureAlbum createOrUpdateSystemHotAlbum(String category, String albumName, Long systemUserId);
    
    /**
     * 使用热门图片更新收藏夹
     * 
     * @param albumId 收藏夹ID
     * @param hotPictures 热门图片列表
     */
    void updateAlbumWithHotPictures(Long albumId, List<Picture> hotPictures);
    
    /**
     * 为用户推荐感兴趣的收藏夹
     * 基于用户的历史行为（点赞、收藏）分析兴趣偏好
     * 
     * @param user 用户对象
     * @param limit 返回数量限制
     * @return 推荐的收藏夹列表
     */
    List<PictureAlbum> getRecommendedAlbumsForUser(User user, int limit);
    
    /**
     * 保存用户推荐收藏夹到缓存
     * 
     * @param userId 用户ID
     * @param recommendedAlbums 推荐收藏夹列表
     * @return boolean 是否保存成功
     */
    boolean saveUserRecommendedAlbums(Long userId, List<PictureAlbum> recommendedAlbums);
    
    /**
     * 从缓存中获取用户推荐收藏夹
     * 
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 用户推荐收藏夹列表
     */
    List<PictureAlbum> getUserRecommendedAlbumsFromCache(Long userId, int limit);
}