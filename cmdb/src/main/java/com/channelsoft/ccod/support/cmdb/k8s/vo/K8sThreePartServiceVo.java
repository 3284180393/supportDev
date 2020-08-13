package com.channelsoft.ccod.support.cmdb.k8s.vo;

import io.kubernetes.client.openapi.models.V1Endpoints;
import io.kubernetes.client.openapi.models.V1Service;

/**
 * @ClassName: K8sThreePartServiceVo
 * @Author: lanhb
 * @Description: 用来定义第三方服务的k8s信息
 * @Date: 2020/8/13 10:52
 * @Version: 1.0
 */
public class K8sThreePartServiceVo {

    private String serviceName; //服务名

    private V1Service service; //该服务对应的k8s service对象

    private V1Endpoints endpoints; //该服务对应的k8s endpoints对象

    public K8sThreePartServiceVo(String serviceName, V1Service service, V1Endpoints endpoints)
    {
        this.serviceName = serviceName;
        this.service = service;
        this.endpoints = endpoints;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public V1Service getService() {
        return service;
    }

    public void setService(V1Service service) {
        this.service = service;
    }

    public V1Endpoints getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(V1Endpoints endpoints) {
        this.endpoints = endpoints;
    }
}
