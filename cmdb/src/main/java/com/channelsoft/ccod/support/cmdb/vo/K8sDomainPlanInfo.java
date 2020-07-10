package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.DomainType;
import com.channelsoft.ccod.support.cmdb.constant.DomainUpdateType;
import com.channelsoft.ccod.support.cmdb.constant.UpdateStatus;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: K8sDomainPlanInfo
 * @Author: lanhb
 * @Description: 用来定义k8s域升级方案
 * @Date: 2020/7/10 14:59
 * @Version: 1.0
 */
public class K8sDomainPlanInfo {

    private String domainName; //对应的域名

    private String domainId; //对应的域标识

    private DomainType domainType = DomainType.K8S_CONTAINER; //域类型

    private String bkSetName; //域归属的set名

    private List<K8sAppAssembleInfo> appAssembleList; //应用升级操作列表

    private DomainUpdateType updateType; //该域升级方案类型,由DomainUpdateType枚举定义

    private UpdateStatus status; //该升级方案当前状态,由DomainUpdateStatus枚举定义

    private String comment; //备注

    private int occurs; //域的设计并发数

    private int maxOccurs; //域的最大并发数

    private String tags; //域的标签,例如:入呼叫、外呼、

    private List<AppFileNexusInfo> publicConfig; //用来存放域公共配置

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

    public DomainType getDomainType() {
        return domainType;
    }

    public void setDomainType(DomainType domainType) {
        this.domainType = domainType;
    }

    public String getBkSetName() {
        return bkSetName;
    }

    public void setBkSetName(String bkSetName) {
        this.bkSetName = bkSetName;
    }

    public List<K8sAppAssembleInfo> getAppAssembleList() {
        return appAssembleList;
    }

    public void setAppAssembleList(List<K8sAppAssembleInfo> appAssembleList) {
        this.appAssembleList = appAssembleList;
    }

    public DomainUpdateType getUpdateType() {
        return updateType;
    }

    public void setUpdateType(DomainUpdateType updateType) {
        this.updateType = updateType;
    }

    public UpdateStatus getStatus() {
        return status;
    }

    public void setStatus(UpdateStatus status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getOccurs() {
        return occurs;
    }

    public void setOccurs(int occurs) {
        this.occurs = occurs;
    }

    public int getMaxOccurs() {
        return maxOccurs;
    }

    public void setMaxOccurs(int maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public List<AppFileNexusInfo> getPublicConfig() {
        return publicConfig;
    }

    public void setPublicConfig(List<AppFileNexusInfo> publicConfig) {
        this.publicConfig = publicConfig;
    }
}
