package com.channelsoft.ccod.support.cmdb;

import com.channelsoft.ccod.support.cmdb.service.IPlatformAppCollectService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.channelsoft.ccod.support.cmdb.dao")
@SpringBootApplication
public class CmdbApplication {

    @Autowired
    static IPlatformAppCollectService platformAppCollectService;
    public static void main(String[] args) {
        SpringApplication.run(CmdbApplication.class, args);
    }

}
