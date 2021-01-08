package com.channelsoft.ccod.support.cmdb.k8s.vo;

import io.kubernetes.client.openapi.models.*;

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

    private List<V1StatefulSet> statefulSets; //关联k8s的statefulSet

    private List<V1Service> services; //关联k8s的一组服务

    private List<V1Endpoints> endpoints; //引用的外部服务

    private List<V1ConfigMap> configMaps; //对应的第三方应用配置

    private List<ExtensionsV1beta1Ingress> ingresses; //对应的第三方ingress配置

    public K8sThreePartAppVo(
            String appName, String alias, String version, List<V1Deployment> deploys, List<V1StatefulSet> statefulSets,
            List<V1Service> services, List<V1Endpoints> endpoints, List<V1ConfigMap> configMaps, List<ExtensionsV1beta1Ingress> ingresses)
    {
        this.appName = appName;
        this.alias = alias;
        this.version = version;
        this.deploys = deploys;
        this.statefulSets = statefulSets;
        this.services = services;
        this.endpoints = endpoints;
        this.configMaps = configMaps;
        this.ingresses = ingresses;
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

    public List<V1ConfigMap> getConfigMaps() {
        return configMaps;
    }

    public void setConfigMaps(List<V1ConfigMap> configMaps) {
        this.configMaps = configMaps;
    }

    public List<V1StatefulSet> getStatefulSets() {
        return statefulSets;
    }

    public void setStatefulSets(List<V1StatefulSet> statefulSets) {
        this.statefulSets = statefulSets;
    }

    public List<ExtensionsV1beta1Ingress> getIngresses() {
        return ingresses;
    }

    public void setIngresses(List<ExtensionsV1beta1Ingress> ingresses) {
        this.ingresses = ingresses;
    }
}
