package com.mushan.tucangbackend.manager;

import cn.hutool.json.JSONUtil;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.entity.PictureAlbum;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.service.AiGenHistoryService;
import com.mushan.tucangbackend.service.PictureAlbumService;
import com.mushan.tucangbackend.service.PictureService;
import com.mushan.tucangbackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.mushan.tucangbackend.manager.ScheduledTasksManager.SYSTEM_ADMIN_USER_ID;

/**
 * 定时任务管理器
 * 负责执行各种定时任务
 */
@Component
@Slf4j
public class ScheduledTasksManager {

    @Resource
    private PictureAlbumService pictureAlbumService;
    
    @Resource
    private PictureService pictureService;
    
    @Resource
    private UserService userService;
    
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    // 系统管理员用户ID（需要根据实际情况修改）
    public static final Long SYSTEM_ADMIN_USER_ID = 1L;
    
    // 定义热门图片分类
    private static final List<String> HOT_PICTURE_CATEGORIES = Arrays.asList(
        "风景", "人物", "绘画"
    );

    /**
     * 应用启动时初始化缓存数据
     */
    @PostConstruct
    public void initializeCacheOnStartup() {
        log.info("开始初始化缓存数据");
        try {
            // 初始化每日精选收藏夹缓存
            initializeFeaturedAlbumsCache();
            
            // 初始化热门图片收藏夹缓存
            initializeHotPictureAlbumsCache();
            
            // 初始化系统热门收藏夹缓存
            initializeSystemHotAlbumsCache();
            
            log.info("缓存数据初始化完成");
        } catch (Exception e) {
            log.error("初始化缓存数据时发生错误", e);
        }
    }
    
    /**
     * 初始化每日精选收藏夹缓存
     */
    private void initializeFeaturedAlbumsCache() {
        try {
            log.info("开始初始化每日精选收藏夹缓存");
            // 获取精选收藏夹列表（例如前10个）
            List<PictureAlbum> featuredAlbums = pictureAlbumService.getFeaturedAlbums(10);
            log.info("获取到 {} 个精选收藏夹", featuredAlbums.size());
            
            // 保存每日精选收藏夹到缓存
            boolean saved = pictureAlbumService.saveDailyFeaturedAlbums(featuredAlbums);
            if (saved) {
                log.info("每日精选收藏夹已保存到缓存");
            } else {
                log.warn("每日精选收藏夹保存到缓存失败");
            }
        } catch (Exception e) {
            log.error("初始化每日精选收藏夹缓存时发生错误", e);
        }
    }
    
    /**
     * 初始化系统热门收藏夹缓存
     */
    private void initializeSystemHotAlbumsCache() {
        try {
            log.info("开始初始化系统热门收藏夹缓存");
            // 获取系统热门收藏夹列表
            List<PictureAlbum> hotAlbums = pictureAlbumService.getSystemHotAlbums(10);
            log.info("获取到 {} 个系统热门收藏夹", hotAlbums.size());
            
            // 保存系统热门收藏夹到缓存
            boolean saved = pictureAlbumService.saveSystemHotAlbums(hotAlbums);
            if (saved) {
                log.info("系统热门收藏夹已保存到缓存");
            } else {
                log.warn("系统热门收藏夹保存到缓存失败");
            }
        } catch (Exception e) {
            log.error("初始化系统热门收藏夹缓存时发生错误", e);
        }
    }
    
    /**
     * 初始化热门图片收藏夹缓存
     */
    private void initializeHotPictureAlbumsCache() {
        log.info("开始初始化热门图片收藏夹缓存");
        try {
            // 为每个分类创建或更新热门图片收藏夹
            for (String category : HOT_PICTURE_CATEGORIES) {
                try {
                    String albumName = "热门" + category + "图片";
                    // 创建或更新系统热门收藏夹
                    PictureAlbum hotAlbum = pictureAlbumService.createOrUpdateSystemHotAlbum(
                        category, albumName, SYSTEM_ADMIN_USER_ID);
                    log.info("已创建/更新热门{}图片收藏夹，ID: {}", category, hotAlbum.getId());
                    
                    // 获取该分类下按热度排序的图片（基于点赞数和收藏数的权重算法）
                    List<Picture> hotPictures = pictureService.getHotPicturesByPopularity(category, 20);
                    log.info("获取到 {} 张热门{}图片", hotPictures.size(), category);
                    
                    // 更新收藏夹中的图片
                    pictureAlbumService.updateAlbumWithHotPictures(hotAlbum.getId(), hotPictures);
                    log.info("已更新收藏夹 {} 中的图片", hotAlbum.getId());
                    
                } catch (Exception e) {
                    log.error("处理 {} 分类热门图片收藏夹时发生错误", category, e);
                }
            }
            
            log.info("热门图片收藏夹缓存初始化完成");
        } catch (Exception e) {
            log.error("初始化热门图片收藏夹缓存时发生错误", e);
        }
    }

    /**
     * 每日精选收藏夹定时任务
     * 每天凌晨2点执行一次
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyFeaturedAlbumsTask() {
        log.info("开始执行每日精选收藏夹定时任务");
        try {
            // 获取精选收藏夹列表（例如前10个）
            List<PictureAlbum> featuredAlbums = pictureAlbumService.getFeaturedAlbums(10);
            log.info("获取到 {} 个精选收藏夹", featuredAlbums.size());
            
            // 保存每日精选收藏夹到缓存
            boolean saved = pictureAlbumService.saveDailyFeaturedAlbums(featuredAlbums);
            if (saved) {
                log.info("每日精选收藏夹已保存到缓存");
            } else {
                log.warn("每日精选收藏夹保存到缓存失败");
            }
            
            log.info("每日精选收藏夹定时任务执行完成");
        } catch (Exception e) {
            log.error("执行每日精选收藏夹定时任务时发生错误", e);
        }
    }
    
    /**
     * 热门图片收藏夹定时任务
     * 每天凌晨3点执行一次
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void hotPictureAlbumsTask() {
        log.info("开始执行热门图片收藏夹定时任务");
        try {
            // 为每个分类创建或更新热门图片收藏夹
            for (String category : HOT_PICTURE_CATEGORIES) {
                try {
                    String albumName = "热门" + category + "图片";
                    // 创建或更新系统热门收藏夹
                    PictureAlbum hotAlbum = pictureAlbumService.createOrUpdateSystemHotAlbum(
                        category, albumName, SYSTEM_ADMIN_USER_ID);
                    log.info("已创建/更新热门{}图片收藏夹，ID: {}", category, hotAlbum.getId());
                    
                    // 获取该分类下按热度排序的图片（基于点赞数和收藏数的权重算法）
                    List<Picture> hotPictures = pictureService.getHotPicturesByPopularity(category, 20);
                    log.info("获取到 {} 张热门{}图片", hotPictures.size(), category);
                    
                    // 更新收藏夹中的图片
                    pictureAlbumService.updateAlbumWithHotPictures(hotAlbum.getId(), hotPictures);
                    log.info("已更新收藏夹 {} 中的图片", hotAlbum.getId());
                    
                } catch (Exception e) {
                    log.error("处理 {} 分类热门图片收藏夹时发生错误", category, e);
                }
            }
            
            log.info("热门图片收藏夹定时任务执行完成");
        } catch (Exception e) {
            log.error("执行热门图片收藏夹定时任务时发生错误", e);
        }
    }
    
    /**
     * 用户个性化推荐收藏夹定时任务
     * 每天凌晨4点执行一次
     * 为活跃用户预计算个性化推荐收藏夹并缓存
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void userRecommendedAlbumsTask() {
        log.info("开始执行用户个性化推荐收藏夹定时任务");
        try {
            // 获取所有用户（在实际应用中可能需要分页处理）
            List<User> users = userService.list();
            log.info("获取到 {} 个用户", users.size());
            
            int successCount = 0;
            for (User user : users) {
                try {
                    // 为每个用户生成推荐收藏夹（限制为5个）
                    List<PictureAlbum> recommendedAlbums = pictureAlbumService.getRecommendedAlbumsForUser(user, 5);
                    
                    // 将推荐结果存储到Redis缓存中
                    String key = "user:recommended:albums:" + user.getId();
                    ValueOperations<String, String> ops = redisTemplate.opsForValue();
                    
                    // 将推荐收藏夹列表转换为JSON字符串存储
                    String json = JSONUtil.toJsonStr(recommendedAlbums);
                    
                    // 存储到Redis，设置24小时过期时间
                    ops.set(key, json, 24, TimeUnit.HOURS);
                    
                    successCount++;
                } catch (Exception e) {
                    log.error("为用户 {} 生成推荐收藏夹时发生错误", user.getId(), e);
                }
            }
            
            log.info("用户个性化推荐收藏夹定时任务执行完成，成功处理 {} 个用户", successCount);
        } catch (Exception e) {
            log.error("执行用户个性化推荐收藏夹定时任务时发生错误", e);
        }
    }

    /**
     * 定期删除三天外Ai文生图记录
     * 每天凌晨0点执行一次
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void deleteAiTextToImageRecordsTask() {
        log.info("开始执行定期删除三天外Ai文生图记录任务");
        try {
            // 计算三天前的时间
            Date threeDaysAgo = new Date(System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000);
            
            // 删除三天前的记录
            int deletedCount = aiGenHistoryService.deleteBefore(threeDaysAgo);
            log.info("已删除 {} 条三天外的Ai文生图记录", deletedCount);
        } catch (Exception e) {
            log.error("执行定期删除三天外Ai文生图记录任务时发生错误", e);
        }
    }
    
    @Resource
    private AiGenHistoryService aiGenHistoryService;
    
}