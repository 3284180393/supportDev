package com.channelsoft.ccod.support.cmdb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @ClassName: CCODBiz
 * @Author: lanhb
 * @Description: 用来配置ccod的业务平台
 * @Date: 2020/4/29 14:09
 * @Version: 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "ccod")
public class CCODBiz {

    private List<BizSetDefine> set;

    private List<String> notCheckCfgApps;

    public List<BizSetDefine> getSet() {
        return set;
    }

    public void setSet(List<BizSetDefine> set) {
        this.set = set;
    }

    public List<String> getNotCheckCfgApps() {
        return notCheckCfgApps;
    }

    public void setNotCheckCfgApps(List<String> notCheckCfgApps) {
        this.notCheckCfgApps = notCheckCfgApps;
    }
}