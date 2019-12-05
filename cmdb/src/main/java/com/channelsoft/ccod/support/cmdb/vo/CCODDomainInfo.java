package com.channelsoft.ccod.support.cmdb.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: CCODDomainInfo
 * @Author: lanhb
 * @Description: 用来定义ccod的set下的域信息
 * @Date: 2019/12/4 10:25
 * @Version: 1.0
 */
public class CCODDomainInfo {

    private String domainId;  //域id

    private String domainName; //域名

    private int domId; //该域在数据库的唯一主键

    private int bkSetId; //该域归属于蓝鲸paas的set的id

    private List<CCODModuleInfo> modules;

    public CCODDomainInfo(int bkSetId, int domId, String domainId, String domainName)
    {
        this.bkSetId = bkSetId;
        this.domId = domId;
        this.domainId = domainId;
        this.domainName = domainName;
        this.modules = new ArrayList<>();
    }


    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public int getDomId() {
        return domId;
    }

    public void setDomId(int domId) {
        this.domId = domId;
    }

    public int getBkSetId() {
        return bkSetId;
    }

    public void setBkSetId(int bkSetId) {
        this.bkSetId = bkSetId;
    }

    public List<CCODModuleInfo> getModules() {
        return modules;
    }

    public void setModules(List<CCODModuleInfo> modules) {
        this.modules = modules;
    }
}
