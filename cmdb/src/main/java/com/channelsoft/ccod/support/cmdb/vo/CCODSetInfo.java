package com.channelsoft.ccod.support.cmdb.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: CCODSetInfo
 * @Author: lanhb
 * @Description: 用来定义ccod平台的set信息，该set对应蓝鲸paas的set概念
 * @Date: 2019/12/4 9:26
 * @Version: 1.0
 */
public class CCODSetInfo {

    private String bkSetName; //该set对应lj paas的set名,该名字也是set在cmdb中的名字

    private List<CCODDomainInfo> domains; //在cmdb的逻辑划分中,该set下面的域

    public CCODSetInfo(String bkSetName)
    {
        this.bkSetName = bkSetName;
        this.domains = new ArrayList<>();
    }

    public String getBkSetName() {
        return bkSetName;
    }

    public void setBkSetName(String bkSetName) {
        this.bkSetName = bkSetName;
    }

    public List<CCODDomainInfo> getDomains() {
        return domains;
    }

    public void setDomains(List<CCODDomainInfo> domains) {
        this.domains = domains;
    }

    @Override
    public CCODSetInfo clone()
    {
        CCODSetInfo setInfo = new CCODSetInfo(this.bkSetName);
        List<CCODDomainInfo> domainList = new ArrayList<>();
        for(CCODDomainInfo domainInfo : this.domains)
        {
            domainList.add(domainInfo.clone());
        }
        setInfo.setDomains(domainList);
        return setInfo;
    }
}
