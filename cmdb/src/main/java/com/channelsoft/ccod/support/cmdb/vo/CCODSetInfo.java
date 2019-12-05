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

    private String bkSetName;

    private int bkSetId;

    private List<CCODDomainInfo> domains;

    public CCODSetInfo(String bkSetName)
    {
        this.bkSetName = bkSetName;
        this.domains = new ArrayList<>();
    }

    public CCODSetInfo(LJSetInfo bkSet)
    {
        this.bkSetId = bkSet.getSetId();
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

}
