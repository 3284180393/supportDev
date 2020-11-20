package com.channelsoft.ccod.support.cmdb.k8s.vo;

import io.kubernetes.client.openapi.models.ExtensionsV1beta1Ingress;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Endpoints;
import io.kubernetes.client.openapi.models.V1Service;

import java.util.List;

/**
 * @ClassName: K8sThreePartAppVo
 * @Author: lanhb
 * @Description: 用来定义ccod平台依赖的第三方应用k8s相关信息
 * @Date: 2020/8/13 10:47
 * @Version: 1.0
 */
public class K8sThreePartAppVo {

    private String appName;  //应用名

    private String alias; //应用别名

    private String version; //版本

    private List<V1Deployment> deploys; //关联k8s的deployment

    private List<V1Service> services; //关联k8s的一组服务

    private List<V1Endpoints> endpoints; //引用的外部服务

    public K8sThreePartAppVo(String appName, String alias, String version, List<V1Deployment> deploys, List<V1Service> services, List<V1Endpoints> endpoints)
    {
        this.appName = appName;
        this.alias = alias;
        this.version = version;
        this.deploys = deploys;
        this.services = services;
        this.endpoints = endpoints;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<V1Deployment> getDeploys() {
        return deploys;
    }

    public void setDeploys(List<V1Deployment> deploys) {
        this.deploys = deploys;
    }

    public List<V1Service> getServices() {
        return services;
    }

    public void setServices(List<V1Service> services) {
        this.services = services;
    }

    public List<V1Endpoints> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<V1Endpoints> endpoints) {
        this.endpoints = endpoints;
    }
}
