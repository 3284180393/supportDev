package com.channelsoft.ccod.support.cmdb.ci.ontimer;

import com.channelsoft.ccod.support.cmdb.service.IPlatformManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @ClassName: AppDebugOntimer
 * @Author: lanhb
 * @Description: 用来定义ccod应用调试定时器
 * @Date: 2020/11/3 19:58
 * @Version: 1.0
 */
@Component
@EnableScheduling
public class AppDebugOntimer {

    private final static Logger logger = LoggerFactory.getLogger(AppDebugOntimer.class);

    @Autowired
    IPlatformManagerService platformManagerService;

    @Scheduled(cron = "${jobs.app-debug.cron}")
    public void start()
    {
        logger.debug(String.format("app debug ontimer start"));
        try{
            platformManagerService.debugHandle();
        }
        catch (Exception ex)
        {
            logger.error(String.format("handle app debug ontimer exception", ex));
        }
        logger.info(String.format("app debug ontimer end"));
    }

}
