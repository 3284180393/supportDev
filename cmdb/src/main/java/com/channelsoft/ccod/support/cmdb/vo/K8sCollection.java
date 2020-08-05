package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.AppType;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1Ingress;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Service;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: K8sCollection
 * @Author: lanhb
 * @Description: 用来定义k8s集合相关内容
 * @Date: 2020/7/2 19:41
 * @Version: 1.0
 */
public class K8sCollection {

    private String tag;

    private String domainId;

    private AppModuleVo appModule;

    @NotNull(message = "appType of domain plan can not be null")
    private AppType appType;

    @NotNull(message = "appName of domain plan can not be null")
    private String appName;

    @NotNull(message = "alias of domain plan can not be null")
    private String alias;

    @NotNull(message = "version of domain plan can not be null")
    private String version;

    @NotNull(message = "deployment of domain plan can not be null")
    private V1Deployment deployment;

    @NotNull(message = "services of domain plan can not be null")
    private List<V1Service> services;

    @NotNull(message = "ingresses of domain plan can not be null")
    private List<ExtensionsV1beta1Ingress> ingresses;

    private V1ConfigMap configMap;

    private int nodePort;

    private int timeout;

    public K8sCollection()
    {}

    public K8sCollection(String appName, String alias, AppType appType, V1Deployment deploy)
    {
        this.appName = appName;
        this.alias = alias;
        this.appType = appType;
        this.deployment = deploy;
        this.services = new ArrayList<>();
        this.ingresses = new ArrayList<>();
    }

    public K8sCollection(String domainId, AppModuleVo appModule, String alias, V1Deployment deployment, List<V1Service> services, List<ExtensionsV1beta1Ingress> ingresses)
    {
        this.domainId = domainId;
        this.appModule = appModule;
        this.appName = appModule.getAppName();
        this.alias = alias;
        this.version = appModule.getVersion();
        this.appType = appModule.getAppType();
        this.deployment = deployment;
        this.services = services;
        this.ingresses = ingresses;
    }

    public K8sCollection(V1Deployment deployment)
    {
        this.deployment = deployment;
        this.services = new ArrayList<>();
        this.ingresses = new ArrayList<>();
        this.timeout = 0;
    }

    public K8sCollection(V1Deployment deployment, int timeout)
    {
        this.deployment = deployment;
        this.services = new ArrayList<>();
        this.ingresses = new ArrayList<>();
        this.timeout = timeout;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
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

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getNodePort() {
        return nodePort;
    }

    public void setNodePort(int nodePort) {
        this.nodePort = nodePort;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public AppType getAppType() {
        return appType;
    }

    public void setAppType(AppType appType) {
        this.appType = appType;
    }

    public AppModuleVo getAppModule() {
        return appModule;
    }

    public void setAppModule(AppModuleVo appModule) {
        this.appModule = appModule;
    }

    public V1ConfigMap getConfigMap() {
        return configMap;
    }

    public void setConfigMap(V1ConfigMap configMap) {
        this.configMap = configMap;
    }
}
