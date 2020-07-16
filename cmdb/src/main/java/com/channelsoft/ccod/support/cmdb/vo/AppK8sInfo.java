package com.channelsoft.ccod.support.cmdb.vo;

import io.kubernetes.client.openapi.models.ExtensionsV1beta1Ingress;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Service;

import java.util.List;

/**
 * @ClassName: AppK8sInfo
 * @Author: lanhb
 * @Description: 用来定义app和k8s相关之间关系的类
 * @Date: 2020/7/16 17:56
 * @Version: 1.0
 */
public class AppK8sInfo {

    private String platformId;

    private String domainId;

    private AppUpdateOperationInfo optInfo;

    private V1Deployment deployment;

    private List<V1Service> services;

    private List<ExtensionsV1beta1Ingress> ingresses;

    private V1Deployment srcDeployment;

    private List<V1Service> srcServices;

    private List<ExtensionsV1beta1Ingress> srcIngresses;

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public V1Deployment getDeployment() {
        return deployment;
    }

    public void setDeployment(V1Deployment deployment) {
        this.deployment = deployment;
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

    public AppUpdateOperationInfo getOptInfo() {
        return optInfo;
    }

    public void setOptInfo(AppUpdateOperationInfo optInfo) {
        this.optInfo = optInfo;
    }

    public V1Deployment getSrcDeployment() {
        return srcDeployment;
    }

    public void setSrcDeployment(V1Deployment srcDeployment) {
        this.srcDeployment = srcDeployment;
    }

    public List<V1Service> getSrcServices() {
        return srcServices;
    }

    public void setSrcServices(List<V1Service> srcServices) {
        this.srcServices = srcServices;
    }

    public List<ExtensionsV1beta1Ingress> getSrcIngresses() {
        return srcIngresses;
    }

    public void setSrcIngresses(List<ExtensionsV1beta1Ingress> srcIngresses) {
        this.srcIngresses = srcIngresses;
    }
}
