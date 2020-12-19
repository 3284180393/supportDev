package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.DomainStatus;
import com.channelsoft.ccod.support.cmdb.constant.DomainType;
import com.channelsoft.ccod.support.cmdb.constant.DomainUpdateType;
import com.channelsoft.ccod.support.cmdb.constant.UpdateStatus;
import com.channelsoft.ccod.support.cmdb.po.DomainPo;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1Ingress;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: DomainUpdatePlanInfo
 * @Author: lanhb
 * @Description: 用来定义域升级方案信息的类
 * @Date: 2019/12/11 17:44
 * @Version: 1.0
 */
@Validated
public class DomainUpdatePlanInfo {

    private String domainName; //对应的域名

    private String domainId; //对应的域标识

    private DomainType domainType = DomainType.K8S_CONTAINER; //域类型

    private String bkSetName; //域归属的set名

    @Valid
    private List<AppUpdateOperationInfo> apps; //应用升级操作列表

    @NotNull(message = "domain updateType can not be null")
    private DomainUpdateType updateType; //该域升级方案类型,由DomainUpdateType枚举定义

    @NotNull(message = "status of domain plan can not be null")
    private UpdateStatus status; //该升级方案当前状态,由DomainUpdateStatus枚举定义

    private String comment; //备注

    private int occurs; //域的设计并发数

    private int maxOccurs; //域的最大并发数

    private String tag; //域的标签,例如:入呼叫、外呼、

    private List<AppFileNexusInfo> publicConfig; //用来存放域公共配置

    private List<V1Deployment> deployments;

    private List<V1Service> services;

    private List<ExtensionsV1beta1Ingress> ingresses;

    public List<AppUpdateOperationInfo> getApps() {
        return apps;
    }

    public void setApps(List<AppUpdateOperationInfo> apps) {
        this.apps = apps;
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

    public String getBkSetName() {
        return bkSetName;
    }

    public void setBkSetName(String bkSetName) {
        this.bkSetName = bkSetName;
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

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public List<AppFileNexusInfo> getPublicConfig() {
        return publicConfig;
    }

    public void setPublicConfig(List<AppFileNexusInfo> publicConfig) {
        this.publicConfig = publicConfig;
    }

    public DomainType getDomainType() {
        return domainType;
    }

    public void setDomainType(DomainType domainType) {
        this.domainType = domainType;
    }

    public List<V1Deployment> getDeployments() {
        return deployments;
    }

    public void setDeployments(List<V1Deployment> deployments) {
        this.deployments = deployments;
    }

    public List<V1Service> getServices() {
        return services;
    }

    public void setServices(List<V1Service> services) {
        this.services = services;
    }

    public List<ExtensionsV1beta1Ingress> getIngresses() {
        return ingresses;
    }

    public void setIngresses(List<ExtensionsV1beta1Ingress> ingresses) {
        this.ingresses = ingresses;
    }

    public DomainPo getDomain(String platformId)
    {
        DomainPo po = new DomainPo();
        po.setDomainId(this.domainId);
        po.setBizSetName(this.bkSetName);
        po.setTag(tag);
        po.setOccurs(this.occurs);
        po.setMaxOccurs(this.maxOccurs);
        po.setComment("");
        po.setPlatformId(platformId);
        po.setStatus(DomainStatus.RUNNING.id);
        po.setCreateTime(new Date());
        po.setDomainName(this.domainName);
        po.setUpdateTime(new Date());
        po.setType(this.domainType);
        return po;
    }

    public Map<String, Object> getHostDomainInfo()
    {
        Map<String, Object> info = new HashMap<>();
        info.put("domainId", domainId);
        info.put("domainName", domainName);
        if(publicConfig != null && publicConfig.size() > 0){
            List<Map<String, String>> cfgParams = publicConfig.stream().map(c->c.getHostFileInfo()).collect(Collectors.toList());
            info.put("publicConfig", cfgParams);
        }
        List<Map<String, Object>> appInfos = apps.stream().map(a->a.getHostAppInfo()).collect(Collectors.toList());
        info.put("apps", appInfos);
        return info;
    }
}
