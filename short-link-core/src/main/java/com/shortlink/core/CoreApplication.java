package com.shortlink.core;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.h2.H2ConsoleAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
//import com.zaxxer.hikari.HikariDataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan(
        basePackages = "com.shortlink.core.mapper",
        sqlSessionFactoryRef = "sqlSessionFactory"
)
@EnableFeignClients(basePackages = "com.shortlink.common.client")
@ComponentScan(basePackages = {
        "com.shortlink.core",      // 扫描自己的核心包（Controller、Service都在这里）
        "com.shortlink.common"     // 保留原来的 common
})
public class CoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreApplication.class, args);
    }
}