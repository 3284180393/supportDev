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

    private List<CCODModuleInfo> modules;

    public CCODDomainInfo(String domainId, String domainName)
    {
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

    public List<CCODModuleInfo> getModules() {
        return modules;
    }

    public void setModules(List<CCODModuleInfo> modules) {
        this.modules = modules;
    }

    @Override
    public CCODDomainInfo clone()
    {
        CCODDomainInfo domainInfo = new CCODDomainInfo(this.domainId, this.domainName);
        List<CCODModuleInfo> moduleList = new ArrayList<>();
        for(CCODModuleInfo module : this.modules)
        {
            moduleList.add(module.clone());
        }
        domainInfo.setModules(moduleList);
        return domainInfo;
    }
}
