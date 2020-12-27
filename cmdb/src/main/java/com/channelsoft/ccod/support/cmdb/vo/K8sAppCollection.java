package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.AppUpdateOperation;
import com.channelsoft.ccod.support.cmdb.po.DomainPo;
import com.channelsoft.ccod.support.cmdb.po.PlatformPo;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1Ingress;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: K8sAppCollection
 * @Author: lanhb
 * @Description: 用来封装应用以及对饮的k8s对象
 * @Date: 2020/12/25 10:11
 * @Version: 1.0
 */
public class K8sAppCollection {

    private String name;

    private String appName;

    private PlatformPo platform;

    private DomainPo domain;

    private List<AppUpdateOperationInfo> optList;

    private AppUpdateOperation operation;

    private V1Deployment deployment;

    private List<V1Service> services;

    private List<ExtensionsV1beta1Ingress> ingresses;

    private List<V1ConfigMap> configMaps;

    private int timeout;

    public K8sAppCollection(){
    }

    public K8sAppCollection(List<AppUpdateOperationInfo> optList, DomainPo domain, PlatformPo platform, V1Deployment deployment,
                            List<V1Service> services, List<ExtensionsV1beta1Ingress> ingresses, List<V1ConfigMap> configMaps, int timeout){
        this.operation = optList.get(0).getOperation();
        this.optList = optList;
        this.domain = domain;
        this.platform = platform;
        this.deployment = deployment;
        this.services = services;
        this.ingresses = ingresses;
        this.configMaps = configMaps;
        this.timeout = timeout;
        this.name = String.join("-", optList.stream().map(a->a.getAlias()).collect(Collectors.toList()));
        this.appName = String.join("-", optList.stream().map(a->a.getAppName()).collect(Collectors.toList()));
    }

    public PlatformPo getPlatform() {
        return platform;
    }

    public void setPlatform(PlatformPo platform) {
        this.platform = platform;
    }

    public DomainPo getDomain() {
        return domain;
    }

    public void setDomain(DomainPo domain) {
        this.domain = domain;
    }

    public List<AppUpdateOperationInfo> getOptList() {
        return optList;
    }

    public void setOptList(List<AppUpdateOperationInfo> optList) {
        this.optList = optList;
    }

    public AppUpdateOperation getOperation() {
        return operation;
    }

    public void setOperation(AppUpdateOperation operation) {
        this.operation = operation;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public List<V1ConfigMap> getConfigMaps() {
        return configMaps;
    }

    public void setConfigMaps(List<V1ConfigMap> configMaps) {
        this.configMaps = configMaps;
    }
}
