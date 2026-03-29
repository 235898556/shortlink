package com.shortlink.stats;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient          // 启用服务注册（如 Nacos）
@MapperScan("com.shortlink.stats.mapper")
@ComponentScan(basePackages = {"com.shortlink.common"})
// 扫描 MyBatis Mapper
public class StatsApplication {
    public static void main(String[] args) {
        SpringApplication.run(StatsApplication.class, args);
    }
}