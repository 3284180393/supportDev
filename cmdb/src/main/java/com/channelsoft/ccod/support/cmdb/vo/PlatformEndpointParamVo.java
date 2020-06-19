package com.channelsoft.ccod.support.cmdb.vo;

import io.kubernetes.client.openapi.models.V1Endpoints;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @ClassName: PlatformEndpointParamVo
 * @Author: lanhb
 * @Description: 用来定义平台k8s Endpoints相关参数
 * @Date: 2020/6/19 15:44
 * @Version: 1.0
 */
public class PlatformEndpointParamVo implements Serializable {

    @NotNull(message = "platformId can not be null")
    private String platformId; //平台id，非空

    private String endpointName; //k8s Endpoints名称,可以为空

    @NotNull(message = "endpoints definition can not be null")
    private V1Endpoints endpoints; //k8s Endpoints 相关定义不可为空

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public V1Endpoints getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(V1Endpoints endpoints) {
        this.endpoints = endpoints;
    }
}
