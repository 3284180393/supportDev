package com.channelsoft.ccod.support.cmdb.vo;

import java.util.List;

/**
 * @ClassName: ThreePartAppConfig
 * @Author: lanhb
 * @Description: 用来定义第三方应用的配置项
 * @Date: 2020/12/11 16:32
 * @Version: 1.0
 */
public class ThreePartAppConfig {
    private String name;

    private String alias;

    List<ConfigKey> cfg;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public List<ConfigKey> getCfg() {
        return cfg;
    }

    public void setCfg(List<ConfigKey> cfg) {
        this.cfg = cfg;
    }
}
