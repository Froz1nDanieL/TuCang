package com.mushan.tucangbackend;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class})
@EnableAsync
@EnableScheduling
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.mushan.tucangbackend.mapper")
public class TuCangBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TuCangBackendApplication.class, args);
    }

}
