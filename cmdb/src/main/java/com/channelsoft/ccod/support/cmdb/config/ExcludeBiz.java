package com.channelsoft.ccod.support.cmdb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: ExcludeBiz
 * @Author: lanhb
 * @Description: 定义用来排除的蓝鲸paas的biz名字
 * @Date: 2019/12/18 15:42
 * @Version: 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "lj-paas")
public class ExcludeBiz {
    private List<String> excludes = new ArrayList<>();

    public List<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }
}
