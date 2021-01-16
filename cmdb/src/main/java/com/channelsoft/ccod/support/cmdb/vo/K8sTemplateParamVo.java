package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.AppType;

import java.util.List;

/**
 * @ClassName: K8sTemplateParamVo
 * @Author: lanhb
 * @Description: 用来定义k8s模板关联参数
 * @Date: 2021/1/13 16:42
 * @Version: 1.0
 */
public class K8sTemplateParamVo {
    AppType appType;
    String appName;
    String alias;
    String version;
    List<String> deploymentNames;
    List<String> statefulSetNames;
    List<String> serviceNames;
    List<String> endpointNames;
    List<String> ingressNames;
    List<String> configMapNames;
    List<String> secretNames;
    String platformId;
    String domainId;
    String hostUrl;
    String k8sApiUrl;
    String k8sApiAuthToken;

    public AppType getAppType() {
        return appType;
    }

    public void setAppType(AppType appType) {
        this.appType = appType;
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

    public List<String> getDeploymentNames() {
        return deploymentNames;
    }

    public void setDeploymentNames(List<String> deploymentNames) {
        this.deploymentNames = deploymentNames;
    }

    public List<String> getStatefulSetNames() {
        return statefulSetNames;
    }

    public void setStatefulSetNames(List<String> statefulSetNames) {
        this.statefulSetNames = statefulSetNames;
    }

    public List<String> getServiceNames() {
        return serviceNames;
    }

    public void setServiceNames(List<String> serviceNames) {
        this.serviceNames = serviceNames;
    }

    public List<String> getEndpointNames() {
        return endpointNames;
    }

    public void setEndpointNames(List<String> endpointNames) {
        this.endpointNames = endpointNames;
    }

    public List<String> getIngressNames() {
        return ingressNames;
    }

    public void setIngressNames(List<String> ingressNames) {
        this.ingressNames = ingressNames;
    }

    public List<String> getConfigMapNames() {
        return configMapNames;
    }

    public void setConfigMapNames(List<String> configMapNames) {
        this.configMapNames = configMapNames;
    }

    public List<String> getSecretNames() {
        return secretNames;
    }

    public void setSecretNames(List<String> secretNames) {
        this.secretNames = secretNames;
    }

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

    public String getHostUrl() {
        return hostUrl;
    }

    public void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
    }

    public String getK8sApiUrl() {
        return k8sApiUrl;
    }

    public void setK8sApiUrl(String k8sApiUrl) {
        this.k8sApiUrl = k8sApiUrl;
    }

    public String getK8sApiAuthToken() {
        return k8sApiAuthToken;
    }

    public void setK8sApiAuthToken(String k8sApiAuthToken) {
        this.k8sApiAuthToken = k8sApiAuthToken;
    }
}
