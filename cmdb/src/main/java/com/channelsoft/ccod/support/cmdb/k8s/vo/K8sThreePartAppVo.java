package com.channelsoft.ccod.support.cmdb.k8s.vo;

import io.kubernetes.client.openapi.models.ExtensionsV1beta1Ingress;
import io.kubernetes.client.openapi.models.V1Deployment;
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

    private V1Deployment deploy; //关联k8s的deployment

    private List<V1Service> services; //关联k8s的一组服务

    private ExtensionsV1beta1Ingress ingress; //如果有关联的ingress，关联的ingress

    public K8sThreePartAppVo(String appName, String alias, String version, V1Deployment deploy, List<V1Service> services, ExtensionsV1beta1Ingress ingress)
    {
        this.appName = appName;
        this.alias = alias;
        this.version = version;
        this.deploy = deploy;
        this.services = services;
        this.ingress = ingress;
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

    public ExtensionsV1beta1Ingress getIngress() {
        return ingress;
    }

    public void setIngress(ExtensionsV1beta1Ingress ingress) {
        this.ingress = ingress;
    }
}
