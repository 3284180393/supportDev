package com.channelsoft.ccod.support.cmdb.vo;

import io.kubernetes.client.openapi.models.ExtensionsV1beta1Ingress;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Service;

import java.util.List;

/**
 * @ClassName: K8sSetCollection
 * @Author: lanhb
 * @Description: 用来定义k8s集群相关内容
 * @Date: 2020/7/2 17:45
 * @Version: 1.0
 */
public class K8sSetCollection {

    private String tag;

    private V1Deployment deployment;

    private List<V1Service> services;

    private List<ExtensionsV1beta1Ingress> ingresses;

    private int timeout;

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
}
