package com.channelsoft.ccod.support.cmdb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @ClassName: CCODBizSet
 * @Author: lanhb
 * @Description: 用来配置ccod平台的业务集群
 * @Date: 2020/4/29 14:01
 * @Version: 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "lj-paas.set-apps")
public class CCODBizSet {

    private List<BizSetDefine> set;

    public List<BizSetDefine> getSet() {
        return set;
    }

    public void setSet(List<BizSetDefine> set) {
        this.set = set;
    }
}
