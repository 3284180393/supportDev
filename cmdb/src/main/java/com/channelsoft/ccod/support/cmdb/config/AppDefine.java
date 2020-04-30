package com.channelsoft.ccod.support.cmdb.config;

import org.apache.commons.lang3.StringUtils;

/**
 * @ClassName: AppDefine
 * @Author: lanhb
 * @Description: 用来定义app相关配置
 * @Date: 2020/4/29 11:14
 * @Version: 1.0
 */
public class AppDefine {

    private String name;  //应用名

    private String alias; //应用的标准别名

    private int delay; //当应用启动后需要延迟多少时间才启动其它的应用

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        if(StringUtils.isBlank(this.alias))
            return this.name.toLowerCase();
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }
}
