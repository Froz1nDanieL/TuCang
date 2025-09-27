package com.mushan.tucangbackend;

import com.mushan.tucangbackend.manager.ScheduledTasksManager;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class TuCangBackendApplicationTests {

    @Resource
    private UserService userService;
    
    @Resource
    private ScheduledTasksManager scheduledTasksManager;

    @Test
    void batchInsertUsers() {

    }

    @Test
    void initHotPictureAlbums() {
        // 手动初始化热门图片收藏夹，无需等待定时任务执行
        scheduledTasksManager.hotPictureAlbumsTask();
    }
    
    @Test
    void initUserRecommendedAlbums() {
        // 手动执行用户个性化推荐收藏夹任务，无需等待定时任务执行
        scheduledTasksManager.userRecommendedAlbumsTask();
    }

    @Test
    void contextLoads() {
    }

}