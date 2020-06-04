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

    private List<CCODAssembleInfo> assembles;

    public CCODDomainInfo(String domainId, String domainName)
    {
        this.domainId = domainId;
        this.domainName = domainName;
        this.assembles = new ArrayList<>();
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

    public List<CCODAssembleInfo> getAssembles() {
        return assembles;
    }

    public void setAssembles(List<CCODAssembleInfo> assembles) {
        this.assembles = assembles;
    }

    @Override
    public CCODDomainInfo clone()
    {
        CCODDomainInfo domainInfo = new CCODDomainInfo(this.domainId, this.domainName);
        List<CCODAssembleInfo> assembleList = new ArrayList<>();
        for(CCODAssembleInfo assembleInfo : this.assembles)
        {
            assembleList.add(assembleInfo.clone());
        }
        domainInfo.setAssembles(assembleList);
        return domainInfo;
    }
}
