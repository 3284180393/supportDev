package com.channelsoft.ccod.support.cmdb.k8s.vo;

import com.channelsoft.ccod.support.cmdb.vo.AppModuleVo;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1Ingress;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Service;

import java.util.List;

/**
 * @ClassName: K8sCCODDomainAppVo
 * @Author: lanhb
 * @Description: 用来定义同ccod域应用k8s相关信息的类
 * @Date: 2020/8/13 10:50
 * @Version: 1.0
 */
public class K8sCCODDomainAppVo {

    private String domainId; //部署该应用的域id

    private String alias; //该应用在域里面的别名

    private AppModuleVo appModule; //ccod应用的注册信息

    private V1ConfigMap configMap; //关联k8s的configMap

    private V1Deployment deploy; //关联k8s的deployment

    private List<V1Service> services; //关联k8s的一组服务

    private List<ExtensionsV1beta1Ingress> ingresses; //如果有关联的ingress，关联的ingress

    public K8sCCODDomainAppVo(String alias, AppModuleVo appModule, String domainId, V1ConfigMap configMap, V1Deployment deploy, List<V1Service> services, List<ExtensionsV1beta1Ingress> ingresses)
    {
        this.alias = alias;
        this.appModule = appModule;
        this.domainId = domainId;
        this.configMap = configMap;
        this.deploy = deploy;
        this.services = services;
        this.ingresses = ingresses;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public AppModuleVo getAppModule() {
        return appModule;
    }

    public void setAppModule(AppModuleVo appModule) {
        this.appModule = appModule;
    }

    public V1Deployment getDeploy() {
        return deploy;
    }

    public void setDeploy(V1Deployment deploy) {
        this.deploy = deploy;
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

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getAppName()
    {
        return this.appModule.getAppName();
    }

    public String getVersion()
    {
        return this.appModule.getVersion();
    }

    public V1ConfigMap getConfigMap() {
        return configMap;
    }

    public void setConfigMap(V1ConfigMap configMap) {
        this.configMap = configMap;
    }

    public String getAlias() {
        return alias;
    }

    public String getCCODVersion()
    {
        return this.appModule.getCcodVersion();
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s(%s)] at %s", alias, this.appModule.getAppName(), this.appModule.getVersion(), this.domainId);
    }
}
