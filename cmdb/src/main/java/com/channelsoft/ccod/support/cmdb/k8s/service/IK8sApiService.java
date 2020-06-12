package com.channelsoft.ccod.support.cmdb.k8s.service;

import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;
import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;


import java.io.IOException;
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

    /**
     * 查询指定命名空间下指定服务名的服务信息
     * @param namespace 命名空间
     * @param serviceName 服务名
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定条件的服务信息
     * @throws ApiException 查询失败
     */
    V1Service queryService(String namespace, String serviceName, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定命名空间下所有服务
     * @param namespace 指定的命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 该命名空间下所有服务
     * @throws ApiException 查询失败
     */
    List<V1Service> queryAllServiceAtNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询所有node
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定命名空间下的所有node
     * @throws ApiException
     */
    List<V1Node> queryAllNode(String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定的node信息
     * @param nodeName node名
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定条件的node信息
     * @throws ApiException 查询失败
     */
    V1Node queryNode(String nodeName, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询命名空间下的所有ConfigMap
     * @param namespace 指定的命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定命名空间下的所有ConfigMap
     * @throws ApiException
     */
    List<V1ConfigMap> queryAllConfigMapAtNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException;


    /**
     * 查询指定命名空间下指定服名的ConfigMap信息
     * @param namespace 命名空间
     * @param configMapName ConfigMap名
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定条件的ConfigMap信息
     * @throws ApiException 查询失败
     */
    V1ConfigMap queryConfigMap(String namespace, String configMapName, String k8sApiUrl, String authToken) throws ApiException;


    /**
     * 查询命名空间下的所有Deployment
     * @param namespace 指定的命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定命名空间下的所有Deployment
     * @throws ApiException
     * @throws ApiException
     */
    List<V1Deployment> queryAllDeploymentAtNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定命名空间下指定服名的Deployment信息
     * @param namespace 命名空间
     * @param deploymentName Deployment名
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定条件的Deployment信息
     * @throws ApiException 查询失败
     */
    V1Deployment queryDeployment(String namespace, String deploymentName, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定命名空间下的所有ingress
     * @param namespace 指定命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 查询结果
     * @throws ApiException
     */
    List<ExtensionsV1beta1Ingress> queryAllIngressAtNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定命名空间下的指定名称的ingress
     * @param namespace 命名空间
     * @param ingressName ingress的名称
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定namespace的ingress信息
     * @throws ApiException
     */
    ExtensionsV1beta1Ingress queryIngress(String namespace, String ingressName, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定命名空间下的所有endpoints
     * @param namespace 指定命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 查询结果
     * @throws ApiException
     */
    List<V1Endpoints> queryAllEndpointsAtNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定命名空间下的指定名称的ingress
     * @param namespace 命名空间
     * @param endpointsName endpoints的名称
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定namespace的ingress信息
     * @throws ApiException
     */
    V1Endpoints queryEndpoints(String namespace, String endpointsName, String k8sApiUrl, String authToken) throws ApiException;

    /**
     *
     * @param namespace
     * @param configMapName
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @param cfgs
     * @param nexusHostUrl
     * @param nexusUser
     * @param nexusPwd
     * @return
     */
    V1ConfigMap createConfigMapFromNexus(String namespace, String configMapName, String k8sApiUrl, String authToken, List<NexusAssetInfo> cfgs, String nexusHostUrl, String nexusUser, String nexusPwd) throws ApiException, InterfaceCallException, IOException;

    /**
     * 删除已经存在的configMap
     * @param namespace 命名空间
     * @param configMapName configMap名称
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @throws ApiException
     */
    void deleteConfigMapByName(String namespace, String configMapName, String k8sApiUrl, String authToken) throws ApiException;
}
