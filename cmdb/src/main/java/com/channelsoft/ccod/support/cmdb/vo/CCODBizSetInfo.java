package com.channelsoft.ccod.support.cmdb.vo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @ClassName: CCODBizSetInfo
 * @Author: lanhb
 * @Description: 用来定义一个合法的ccod biz所必须有的以及可能有的set信息
 * @Date: 2019/12/10 10:11
 * @Version: 1.0
 */

@Configuration
@ConfigurationProperties(prefix = "lj-paas.set-apps")
public class CCODBizSetInfo {

    private List<BizSetDefine> set;

    public List<BizSetDefine> getSet() {
        return set;
    }

    public void setSet(List<BizSetDefine> set) {
        this.set = set;
    }
}
