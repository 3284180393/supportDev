package com.channelsoft.ccod.support.cmdb.k8s.service;

import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;
import com.channelsoft.ccod.support.cmdb.exception.K8SException;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Pod;


import java.util.List;

/**
 * @ClassName: IK8sApiService
 * @Author: lanhb
 * @Description: k8s的api接口类
 * @Date: 2020/5/27 17:42
 * @Version: 1.0
 */
public interface IK8sApiService {
    /**
     * 查询指定名称的命名空间
     * @param namespace 命名空间名称
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定名称的命名空间
     * @throws ApiException 调用k8s api时返回异常
     */
    V1Namespace queryNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询k8s上所有的命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return k8s上所有命名空间
     * @throws ApiException 调用k8s api时返回异常
     */
    List<V1Namespace> queryAllNamespace(String k8sApiUrl, String authToken) throws ApiException;
    /**
     * 查询指定条件的pod信息
     * @param namespace pod所属的namespace
     * @param podName pod名字
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定条件的pod信息
     * @throws ApiException 调用k8s api时返回异常
     */
    V1Pod queryPod(String namespace, String podName, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定namespace下所有的pod信息
     * @param namespace 需要查询的namespace
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定namespace下的所有pod信息
     * @throws ApiException 调用k8s api时返回异常
     */
    List<V1Pod> queryAllPodAtNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException;

}
