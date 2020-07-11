package com.channelsoft.ccod.support.cmdb.vo;

import io.kubernetes.client.openapi.models.ExtensionsV1beta1Ingress;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Service;

import java.util.List;
import java.util.Map;

/**
 * @ClassName: K8sDomainPlanParamVo
 * @Author: lanhb
 * @Description: 用来定义和k8s域升级方案相关的参数
 * @Date: 2020/7/11 16:12
 * @Version: 1.0
 */
public class K8sDomainPlanParamVo {

    Map<String, V1Deployment> appDeploymentMap;

    Map<String, List<V1Service>> appServiceMap;

    Map<String, List<ExtensionsV1beta1Ingress>> appIngressMap;

}
