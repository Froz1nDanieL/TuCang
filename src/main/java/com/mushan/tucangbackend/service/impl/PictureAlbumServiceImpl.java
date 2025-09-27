package com.mushan.tucangbackend.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mushan.tucangbackend.mapper.UserPictureInteractionMapper;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.entity.PictureAlbum;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.entity.UserPictureInteraction;
import com.mushan.tucangbackend.service.PictureAlbumService;
import com.mushan.tucangbackend.mapper.PictureAlbumMapper;
import com.mushan.tucangbackend.service.PictureService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import cn.hutool.json.JSONUtil;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author Danie
* @description 针对表【picture_album】的数据库操作Service实现
* @createDate 2025-08-27 23:48:57
*/
@Service
public class PictureAlbumServiceImpl extends ServiceImpl<PictureAlbumMapper, PictureAlbum>
    implements PictureAlbumService{

    @Resource
    private RedisTemplate<String, String> redisTemplate;
    
    @Resource
    private UserPictureInteractionMapper userPictureInteractionMapper;
    
    @Resource
    private PictureService pictureService;

    @Override
    public boolean increaseViewCount(Long albumId) {
        return this.lambdaUpdate()
                .eq(PictureAlbum::getId, albumId)
                .setSql("viewCount = viewCount + 1")
                .update();
    }

    @Override
    public List<PictureAlbum> getFeaturedAlbums(int limit) {
        // 基于复合权重的算法计算精选收藏夹
        // 热度 = 浏览数*0.5 + 图片数*0.3 + 时间因子*0.2
        QueryWrapper<PictureAlbum> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "name", "description", "userId", "isPublic", 
                           "pictureCount", "viewCount", "createTime",
                           "(viewCount * 0.5 + pictureCount * 0.3 + CASE WHEN DATEDIFF(NOW(), createTime) <= 7 THEN 10 ELSE 0 END * 0.2) AS score")
                .eq("isPublic", 1)
                .ne("userId", 1) // 排除系统管理员（userId为1）创建的收藏夹
                .gt("pictureCount", 0)
                .apply("(viewCount * 0.5 + pictureCount * 0.3 + CASE WHEN DATEDIFF(NOW(), createTime) <= 7 THEN 10 ELSE 0 END * 0.2) > 0")
                .orderByDesc("(viewCount * 0.5 + pictureCount * 0.3 + CASE WHEN DATEDIFF(NOW(), createTime) <= 7 THEN 10 ELSE 0 END * 0.2)")
                .last("LIMIT " + limit);
        
        return this.list(queryWrapper);
    }
    
    @Override
    public boolean saveDailyFeaturedAlbums(List<PictureAlbum> featuredAlbums) {
        // 将每日精选收藏夹存储到Redis缓存中
        String key = "daily:featured:albums:" + formatDate(new Date());
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        
        // 将精选收藏夹列表转换为JSON字符串存储
        String json = JSONUtil.toJsonStr(featuredAlbums);
        
        // 存储到Redis，设置24小时过期时间
        ops.set(key, json, 24, TimeUnit.HOURS);
        
        return true;
    }
    
    /**
     * 从缓存中获取今日精选收藏夹
     * 
     * @param limit 限制数量
     * @return 今日精选收藏夹列表
     */
    @Override
    public List<PictureAlbum> getTodayFeaturedAlbumsFromCache(int limit) {
        String key = "daily:featured:albums:" + formatDate(new Date());
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        
        String json = ops.get(key);
        if (json != null) {
            // 从缓存中获取数据
            List<PictureAlbum> albums = JSONUtil.toList(json, PictureAlbum.class);
            
            // 如果limit小于列表大小，则截取前limit个元素
            if (limit < albums.size()) {
                return albums.subList(0, limit);
            }
            return albums;
        }
        
        // 缓存中没有数据，返回null表示需要重新计算
        return null;
    }

    @Override
    public List<PictureAlbum> getHotPicturesByCategory(String category, int limit) {
        // 获取指定分类下的热门图片（根据浏览数排名）
        QueryWrapper<PictureAlbum> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "name", "description", "userId", "isPublic",
                        "pictureCount", "viewCount", "createTime")
                .eq("category", category)
                .eq("isPublic", 1)
                .orderByDesc("viewCount")
                .last("LIMIT " + limit);

        return this.list(queryWrapper);
    }

    /**
     * 格式化日期为yyyy-MM-dd格式
     */
    private String formatDate(Date date) {
        return DateUtil.format(date, "yyyy-MM-dd");
    }

    @Override
    public List<PictureAlbum> getRecommendedAlbumsForUser(User user, int limit) {
        // 1. 获取用户最近的互动记录（点赞、收藏）
        QueryWrapper<UserPictureInteraction> interactionQueryWrapper = new QueryWrapper<>();
        interactionQueryWrapper.eq("userId", user.getId())
                .orderByDesc("createTime")
                .last("LIMIT 100"); // 限制最近100条记录
        
        List<UserPictureInteraction> interactions = userPictureInteractionMapper.selectList(interactionQueryWrapper);
        
        if (interactions.isEmpty()) {
            // 如果用户没有互动记录，返回普通的精选收藏夹
            return getFeaturedAlbums(limit);
        }
        
        // 2. 根据用户互动记录分析兴趣偏好
        // 获取用户互动过的图片ID
        List<Long> pictureIds = interactions.stream()
                .map(UserPictureInteraction::getPictureId)
                .distinct()
                .collect(Collectors.toList());
        
        // 3. 查询这些图片的分类信息
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        pictureQueryWrapper.select("category")
                .in("id", pictureIds)
                .isNotNull("category")
                .groupBy("category")
                .orderByDesc("COUNT(*)")
                .last("LIMIT 5"); // 获取用户最感兴趣的前5个分类
        
        List<Picture> categoryPictures = pictureService.list(pictureQueryWrapper);
        List<String> interestedCategories = categoryPictures.stream()
                .map(Picture::getCategory)
                .collect(Collectors.toList());
        
        if (interestedCategories.isEmpty()) {
            // 如果无法确定用户兴趣，返回普通精选收藏夹
            return getFeaturedAlbums(limit);
        }
        
        // 4. 根据用户感兴趣的分类推荐相关的收藏夹
        // 使用关键词在收藏夹名称和描述中进行模糊匹配
        QueryWrapper<PictureAlbum> albumQueryWrapper = new QueryWrapper<>();
        
        // 构建关键词查询条件
        albumQueryWrapper.eq("isPublic", 1)
                .ne("userId", 1) // 排除系统管理员创建的收藏夹
                .gt("pictureCount", 0);
        
        // 添加关键词模糊匹配条件
        albumQueryWrapper.and(wrapper -> {
            for (String category : interestedCategories) {
                wrapper.like("name", category).or().like("description", category);
            }
        });
        
        albumQueryWrapper.orderByDesc("viewCount")
                .last("LIMIT " + limit);
        
        List<PictureAlbum> candidateAlbums = this.list(albumQueryWrapper);
        
        // 如果通过关键词匹配找到了相关收藏夹，则返回这些收藏夹
        if (!candidateAlbums.isEmpty()) {
            return candidateAlbums;
        }
        
        // 5. 如果没有通过关键词匹配找到相关收藏夹，则回退到基于图片分类的推荐策略
        // 首先找出包含这些分类图片的收藏夹
        QueryWrapper<UserPictureInteraction> albumInteractionQueryWrapper = new QueryWrapper<>();
        albumInteractionQueryWrapper.select("albumId")
                .inSql("pictureId", "SELECT id FROM picture WHERE category IN (" + 
                       interestedCategories.stream().map(category -> "'" + category + "'").collect(Collectors.joining(",")) + 
                       ")")
                .eq("type", 1) // 收藏类型
                .groupBy("albumId")
                .orderByDesc("COUNT(*)")
                .last("LIMIT " + limit);
        
        List<UserPictureInteraction> interactionAlbums = userPictureInteractionMapper.selectList(albumInteractionQueryWrapper);
        List<Long> interactionAlbumIds = interactionAlbums.stream()
                .map(UserPictureInteraction::getAlbumId)
                .collect(Collectors.toList());
        
        if (interactionAlbumIds.isEmpty()) {
            // 如果找不到相关收藏夹，返回普通精选收藏夹
            return getFeaturedAlbums(limit);
        }
        
        // 查询这些收藏夹的详细信息
        QueryWrapper<PictureAlbum> albumDetailQueryWrapper = new QueryWrapper<>();
        albumDetailQueryWrapper.in("id", interactionAlbumIds)
                .eq("isPublic", 1)
                .ne("userId", 1) // 排除系统管理员创建的收藏夹
                .gt("pictureCount", 0)
                .orderByDesc("viewCount")
                .last("LIMIT " + limit);
        
        return this.list(albumDetailQueryWrapper);
    }
    
    @Override
    public boolean saveUserRecommendedAlbums(Long userId, List<PictureAlbum> recommendedAlbums) {
        // 将用户推荐收藏夹存储到Redis缓存中
        String key = "user:recommended:albums:" + userId;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        
        // 将推荐收藏夹列表转换为JSON字符串存储
        String json = JSONUtil.toJsonStr(recommendedAlbums);
        
        // 存储到Redis，设置24小时过期时间
        ops.set(key, json, 24, TimeUnit.HOURS);
        
        return true;
    }
    
    /**
     * 从缓存中获取用户推荐收藏夹
     * 
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 用户推荐收藏夹列表
     */
    @Override
    public List<PictureAlbum> getUserRecommendedAlbumsFromCache(Long userId, int limit) {
        String key = "user:recommended:albums:" + userId;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        
        String json = ops.get(key);
        if (json != null) {
            // 从缓存中获取数据
            List<PictureAlbum> albums = JSONUtil.toList(json, PictureAlbum.class);
            
            // 如果limit小于列表大小，则截取前limit个元素
            if (limit < albums.size()) {
                return albums.subList(0, limit);
            }
            return albums;
        }
        
        // 缓存中没有数据，返回null表示需要重新计算
        return null;
    }

    @Override
    public PictureAlbum createOrUpdateSystemHotAlbum(String category, String albumName, Long systemUserId) {
        // 查找是否已存在该分类的系统热门收藏夹
        QueryWrapper<PictureAlbum> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", albumName)
                .eq("userId", systemUserId);
        
        PictureAlbum album = this.getOne(queryWrapper);
        
        // 如果不存在，则创建新的系统收藏夹
        if (album == null) {
            album = new PictureAlbum();
            album.setName(albumName);
            album.setDescription("热门" + category + "图片收藏夹，每日更新");
            album.setUserId(systemUserId);
            album.setIsPublic(1); // 公开
            album.setPictureCount(0);
            album.setViewCount(0);
            album.setCreateTime(new Date());
            this.save(album);
        }
        
        return album;
    }
    
    @Override
    public void updateAlbumWithHotPictures(Long albumId, List<Picture> hotPictures) {
        try {
            // 先清空收藏夹中的现有图片
            clearAlbumPictures(albumId);
            
            // 将热门图片添加到收藏夹中
            for (Picture picture : hotPictures) {
                addUserPictureInteraction(albumId, picture.getId());
            }
            
            // 更新收藏夹的图片数量
            this.lambdaUpdate()
                    .eq(PictureAlbum::getId, albumId)
                    .set(PictureAlbum::getPictureCount, hotPictures.size())
                    .update();
        } catch (Exception e) {
            throw new RuntimeException("更新收藏夹中的图片时发生错误", e);
        }
    }
    
    /**
     * 清空收藏夹中的图片
     * 
     * @param albumId 收藏夹ID
     */
    private void clearAlbumPictures(Long albumId) {
        QueryWrapper<UserPictureInteraction> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("albumId", albumId)
                .eq("type", 1); // 1 表示收藏类型
        userPictureInteractionMapper.delete(deleteWrapper);
    }
    
    /**
     * 添加用户与图片的交互记录（收藏）
     * 
     * @param albumId 收藏夹ID
     * @param pictureId 图片ID
     */
    private void addUserPictureInteraction(Long albumId, Long pictureId) {
        UserPictureInteraction interaction = new UserPictureInteraction();
        interaction.setUserId(1L); // 系统用户
        interaction.setPictureId(pictureId);
        interaction.setAlbumId(albumId);
        interaction.setType(1); // 1 表示收藏
        interaction.setCreateTime(new java.util.Date());
        interaction.setUpdateTime(new java.util.Date());
        userPictureInteractionMapper.insert(interaction);
    }
}