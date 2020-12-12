package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.DomainUpdateType;

/**
 * @ClassName: YamlDomainVo
 * @Author: lanhb
 * @Description: 用来定义yaml域对象
 * @Date: 2020/12/11 20:10
 * @Version: 1.0
 */
public class YamlDomainVo {

    private String domainName;

    private String domainId;

    private DomainUpdateType updateType;

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }
}
