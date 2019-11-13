package com.channelsoft.ccod.support.cmdb.config;

import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName: MyBatisMapperScannerConfig
 * @Author: lanhb
 * @Description: 用来自动扫描mapper
 * @Date: 2019/11/13 18:03
 * @Version: 1.0
 */
@Configuration
@AutoConfigureAfter(TestConfig.class)
public class MyBatisMapperScannerConfig {

    @Bean
    public MapperScannerConfigurer mapperScannerConfigurer() throws Exception{
        MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
        mapperScannerConfigurer.setBasePackage("com.example.mapper.*;com.gitee.sunchenbin.mybatis.actable.dao.*");
        mapperScannerConfigurer.setSqlSessionFactoryBeanName("sqlSessionFactory");
        return mapperScannerConfigurer;
    }
}
