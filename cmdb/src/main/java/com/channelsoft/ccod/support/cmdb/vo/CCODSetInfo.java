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

    private int bkSetId; //该set对应lj paas上的set id,

    private String setId; //该set在cmdb对应的id

    private List<CCODDomainInfo> domains; //在cmdb的逻辑划分中,该set下面的域

    public CCODSetInfo(String setId, String bkSetName)
    {
        this.bkSetName = bkSetName;
        this.setId = setId;
        this.domains = new ArrayList<>();
    }

    public CCODSetInfo(String setId, LJSetInfo bkSet)
    {
        this.bkSetId = bkSet.getSetId();
        this.setId = setId;
        this.bkSetName = bkSet.getSetName();
        this.domains = new ArrayList<>();
    }

    public String getBkSetName() {
        return bkSetName;
    }

    public void setBkSetName(String bkSetName) {
        this.bkSetName = bkSetName;
    }

    public int getBkSetId() {
        return bkSetId;
    }

    public void setBkSetId(int bkSetId) {
        this.bkSetId = bkSetId;
    }

    public List<CCODDomainInfo> getDomains() {
        return domains;
    }

    public void setDomains(List<CCODDomainInfo> domains) {
        this.domains = domains;
    }

    public String getSetId() {
        return setId;
    }

    public void setSetId(String setId) {
        this.setId = setId;
    }

    @Override
    public CCODSetInfo clone()
    {
        CCODSetInfo setInfo = new CCODSetInfo(this.setId, this.bkSetName);
        setInfo.bkSetId = this.bkSetId;
        List<CCODDomainInfo> domainList = new ArrayList<>();
        for(CCODDomainInfo domainInfo : this.domains)
        {
            domainList.add(domainInfo.clone());
        }
        setInfo.setDomains(domainList);
        return setInfo;
    }
}
