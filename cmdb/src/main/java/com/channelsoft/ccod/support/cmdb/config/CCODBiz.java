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

    private List<String> threePartApps;

    private List<String> threePartServices;

    private StartChains startChains;

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

    public List<String> getThreePartApps() {
        return threePartApps;
    }

    public void setThreePartApps(List<String> threePartApps) {
        this.threePartApps = threePartApps;
    }

    public List<String> getThreePartServices() {
        return threePartServices;
    }

    public void setThreePartServices(List<String> threePartServices) {
        this.threePartServices = threePartServices;
    }

    public StartChains getStartChains() {
        return startChains;
    }

    public void setStartChains(StartChains startChains) {
        this.startChains = startChains;
    }
}
