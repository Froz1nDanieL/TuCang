package com.mushan.tucangbackend;

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

    @Test
    void batchInsertUsers() {

    }

    @Test
    void contextLoads() {
    }

}
