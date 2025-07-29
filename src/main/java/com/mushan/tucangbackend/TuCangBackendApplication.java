package com.mushan.tucangbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.mushan.tucangbackend.mapper")
public class TuCangBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TuCangBackendApplication.class, args);
    }

}
