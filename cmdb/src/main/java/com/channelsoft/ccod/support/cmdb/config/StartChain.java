package com.channelsoft.ccod.support.cmdb.config;

import java.util.List;

/**
 * @ClassName: StartChain
 * @Author: lanhb
 * @Description: 用来定义应用启动链的配置项
 * @Date: 2020/8/4 13:57
 * @Version: 1.0
 */
public class StartChain
{
    private String name;

    private List<String> dependencies;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }
}
