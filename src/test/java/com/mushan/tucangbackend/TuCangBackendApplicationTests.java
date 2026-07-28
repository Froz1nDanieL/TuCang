package com.mushan.tucangbackend;

import com.mushan.tucangbackend.manager.Job.PictureTasksManager;
import com.mushan.tucangbackend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
@Disabled("手动集成测试，默认测试不得连接外部服务")
class TuCangBackendApplicationTests {

    @Resource
    private UserService userService;
    
    @Resource
    private PictureTasksManager pictureTasksManager;

    @Test
    void batchInsertUsers() {

    }

    @Test
    void initHotPictureAlbums() {
        // 手动初始化热门图片收藏夹，无需等待定时任务执行
        pictureTasksManager.hotPictureAlbumsTask();
    }
    
    @Test
    void initUserRecommendedAlbums() {
        // 手动执行用户个性化推荐收藏夹任务，无需等待定时任务执行
        pictureTasksManager.userRecommendedAlbumsTask();
    }

    @Test
    void contextLoads() {
    }

}
