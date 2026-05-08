package com.smartblog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.smartblog.mapper")
@SpringBootApplication
public class SmartblogBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartblogBackendApplication.class, args);
    }
}