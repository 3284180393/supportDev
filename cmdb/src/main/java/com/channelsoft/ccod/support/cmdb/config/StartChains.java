package com.channelsoft.ccod.support.cmdb.config;

import java.util.List;

/**
 * @ClassName: StartChains
 * @Author: lanhb
 * @Description: 用来定义ccod应用启动链的配置
 * @Date: 2020/8/4 14:01
 * @Version: 1.0
 */
public class StartChains {

    List<StartChain> chains;

    public List<StartChain> getChains() {
        return chains;
    }

    public void setChains(List<StartChain> chains) {
        this.chains = chains;
    }
}
