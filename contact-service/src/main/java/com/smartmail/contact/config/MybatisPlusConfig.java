package com.smartmail.contact.config;

import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 显式配置 Mapper 扫描，确保在自定义 DataSource（排除 DataSourceAutoConfiguration）下
 * MyBatis 能正确注册 Mapper Bean，避免 "No MyBatis mapper was found" 导致启动失败。
 */
@Configuration(proxyBeanMethods = false)
public class MybatisPlusConfig {

    @Bean
    public static MapperScannerConfigurer mapperScannerConfigurer() {
        MapperScannerConfigurer configurer = new MapperScannerConfigurer();
        configurer.setBasePackage("com.smartmail.contact.mapper");
        configurer.setSqlSessionFactoryBeanName("sqlSessionFactory");
        return configurer;
    }
}
