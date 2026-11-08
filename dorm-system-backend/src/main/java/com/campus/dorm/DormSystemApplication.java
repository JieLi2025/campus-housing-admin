package com.campus.dorm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园宿舍管理系统 - 主启动类
 * 
 * @author Campus Housing Team
 * @since 2025-10-21
 */
@SpringBootApplication
@MapperScan("com.campus.dorm.mapper")
@EnableAsync
    @EnableScheduling
    public class DormSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(DormSystemApplication.class, args);
        System.out.println("🏠 校园宿舍管理系统启动成功！");
    }
}

