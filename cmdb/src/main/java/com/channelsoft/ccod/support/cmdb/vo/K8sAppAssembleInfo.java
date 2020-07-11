package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.AppType;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1Ingress;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Service;

import java.util.List;

/**
 * @ClassName: K8sAppAssembleInfo
 * @Author: lanhb
 * @Description: 用来定义k8s应用集合相关内容
 * @Date: 2020/7/10 17:54
 * @Version: 1.0
 */
public class K8sAppAssembleInfo {

    private String assembleTag;

    private String assembleName;

    private boolean isThreePartApp;

    private List<AppUpdateOperationInfo> appOptList;

    private V1Deployment deployment;

    private List<V1Service> services;

    private List<ExtensionsV1beta1Ingress> ingresses;

    private int howTo;  //如何处理deployment，0 create，1 replace，2 delete

    private List<V1Service> addServices;

    private List<V1Service> replaceServices;

    private List<V1Service> deleteServices;

    public String getAssembleTag() {
        return assembleTag;
    }

    public void setAssembleTag(String assembleTag) {
        this.assembleTag = assembleTag;
    }

    public String getAssembleName() {
        return assembleName;
    }

    public void setAssembleName(String assembleName) {
        this.assembleName = assembleName;
    }

    public boolean isThreePartApp() {
        return isThreePartApp;
    }

    public void setThreePartApp(boolean threePartApp) {
        isThreePartApp = threePartApp;
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

    public List<AppUpdateOperationInfo> getAppOptList() {
        return appOptList;
    }

    public void setAppOptList(List<AppUpdateOperationInfo> appOptList) {
        this.appOptList = appOptList;
    }
}
