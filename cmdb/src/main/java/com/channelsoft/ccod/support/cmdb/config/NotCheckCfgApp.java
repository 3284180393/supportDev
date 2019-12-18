package com.channelsoft.ccod.support.cmdb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @ClassName: NotCheckCfgApp
 * @Author: lanhb
 * @Description: 用来定义不用检查配置文件数量的应用
 * @Date: 2019/12/18 15:43
 * @Version: 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "ccod")
public class NotCheckCfgApp {

    private List<String> notCheckCfgApps;

    public List<String> getNotCheckCfgApps() {
        return notCheckCfgApps;
    }

    public void setNotCheckCfgApps(List<String> notCheckCfgApps) {
        this.notCheckCfgApps = notCheckCfgApps;
    }
}
