package com.channelsoft.ccod.support.cmdb.k8s.service;

import com.channelsoft.ccod.support.cmdb.constant.K8sStatus;
import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;
import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;


import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: IK8sApiService
 * @Author: lanhb
 * @Description: k8s的api接口类
 * @Date: 2020/5/27 17:42
 * @Version: 1.0
 */
public interface IK8sApiService {

    /**
     * 查询指定条件的pod信息
     * @param name pod名字
     * @param namespace 命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定条件的pod信息
     * @throws ApiException 调用k8s api时返回异常
     */
    V1Pod readNamespacedPod(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定namespace下所有的pod信息
     * @param namespace 需要查询的namespace
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定namespace下的所有pod信息
     * @throws ApiException 调用k8s api时返回异常
     */
    List<V1Pod> listNamespacedPod(String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询所有node
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定命名空间下的所有node
     * @throws ApiException
     */
    List<V1Node> listNode(String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定的node信息
     * @param name node名
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定条件的node信息
     * @throws ApiException 查询失败
     */
    V1Node readNode(String name, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定命名空间下的所有endpoints
     * @param namespace 指定命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 查询结果
     * @throws ApiException
     */
    List<V1Endpoints> listNamespacedEndpoints(String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定Endpoints
     * @param name Endpoints的名称
     * @param namespace 命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定条件的Endpoints信息
     * @throws ApiException
     */
    V1Endpoints readNamespacedEndpoints(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 为指定命名空间创建Endpoints
     * @param namespace 命名空间
     * @param endpoints 需要创建的Endpoints
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 被创建的EndPoints
     * @throws ApiException
     */
    V1Endpoints createNamespacedEndpoints(String namespace, V1Endpoints endpoints, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 替换指定的Endpoints
     * @param name 需要被替换的Endpoints名称
     * @param namespace 被替换的Endpoints所在namespace
     * @param endpoints 替换的Endpoints
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return Endpoints被替换后的结果
     * @throws ApiException
     */
    V1Endpoints replaceNamespacedEndpoints(String name, String namespace, V1Endpoints endpoints, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 删除指定的EndPoints
     * @param name 被删除的Endpoints名称
     * @param namespace 被删除的Endpoints的所在命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 删除结果
     * @throws ApiException
     */
    V1Status deleteNamespacedEndpoints(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定命名空间下的所有secret
     * @param namespace 指定命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 查询结果
     * @throws ApiException
     */
    List<V1Secret> listNamespacedSecret(String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定命名空间下的指定名称的secret
     * @param name secret的名称
     * @param namespace 命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定条件的SECRET信息
     * @throws ApiException
     */
    V1Secret readNamespacedSecret(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 为指定命名空间创建Secret
     * @param namespace 命名空间
     * @param secret 需要创建的Secret
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 被创建的Secret
     * @throws ApiException
     */
    V1Secret createNamespacedSecret(String namespace, V1Secret secret, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 替换指定的Secret
     * @param name 需要被替换的Secret名称
     * @param namespace 被替换的Secret所在namespace
     * @param secret 替换的Secret
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return Secret被替换后的结果
     * @throws ApiException
     */
    V1Secret replaceNamespacedSecret(String name, String namespace, V1Secret secret, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 删除指定的Secret
     * @param name 被删除的Secret名称
     * @param namespace 被删除的Secret的所在命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 删除结果
     * @throws ApiException
     */
    V1Status deleteNamespacedSecret(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定命名空间下的所有job
     * @param namespace 指定命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 查询结果
     * @throws ApiException
     */
    List<V1Job> listNamespacedJob(String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定job
     * @param name job的名称
     * @param namespace 命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定条件的job信息
     * @throws ApiException
     */
    V1Job readNamespacedJob(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 为指定命名空间创建job
     * @param namespace 命名空间
     * @param job 需要创建的job
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 被创建的job
     * @throws ApiException
     */
    V1Job createNamespacedJob(String namespace, V1Job job, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定命名空间下的所有PersistentVolumeClaim
     * @param namespace 指定命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 查询结果
     * @throws ApiException
     */
    List<V1PersistentVolumeClaim> listNamespacedPersistentVolumeClaim(String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定命名空间下的指定名称的PersistentVolumeClaim
     * @param namespace 命名空间
     * @param persistentVolumeClaimName volumeClaim的名称
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定条件的PersistentVolumeClaim信息
     * @throws ApiException
     */
    V1PersistentVolumeClaim readNamespacedPersistentVolumeClaim(String namespace, String persistentVolumeClaimName, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 为指定命名空间创建PersistentVolumeClaim
     * @param namespace 命名空间
     * @param persistentVolumeClaim 需要创建的PersistentVolumeClaim
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 被创建的PersistentVolumeClaim
     * @throws ApiException
     */
    V1PersistentVolumeClaim createNamespacedPersistentVolumeClaim(String namespace, V1PersistentVolumeClaim persistentVolumeClaim, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 替换指定的PersistentVolumeClaim
     * @param name 需要被替换的PersistentVolumeClaim名称
     * @param namespace 被替换的PersistentVolumeClaim所在namespace
     * @param persistentVolumeClaim 替换的PersistentVolumeClaim
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return PersistentVolumeClaim被替换后的结果
     * @throws ApiException
     */
    V1PersistentVolumeClaim replaceNamespacedPersistentVolumeClaim(String name, String namespace, V1PersistentVolumeClaim persistentVolumeClaim, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 删除指定的PersistentVolumeClaim
     * @param name 被删除的PersistentVolumeClaim名称
     * @param namespace 被删除的PersistentVolumeClaim的所在命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 删除结果
     * @throws ApiException
     */
    V1Status deleteNamespacedPersistentVolumeClaim(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询所有PersistentVolume
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return k8s服务器上所有的PersistentVolume
     * @throws ApiException
     */
    List<V1PersistentVolume> listPersistentVolume(String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定的PersistentVolume信息
     * @param name PersistentVolumeName名
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定条件的PersistentVolume信息
     * @throws ApiException 查询失败
     */
    V1PersistentVolume readPersistentVolume(String name, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 向k8s添加新的 PersistentVolume
     * @param persistentVolume 需要添加的PersistentVolume
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 新创建的PersistentVolume信息
     * @throws ApiException 查询失败
     */
    V1PersistentVolume createPersistentVolume(V1PersistentVolume persistentVolume, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 从k8s删除已有的PersistentVolume
     * @param name 需要被删除的PersistentVolume名称
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 删除结果
     * @throws ApiException 查询失败
     */
    V1Status deletePersistentVolume(String name, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 替换已有的PersistentVolume
     * @param name 需要被替换的PersistentVolume的名称
     * @param persistentVolume 用来替换的PersistentVolume
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 替换后的PersistentVolume信息
     * @throws ApiException 查询失败
     */
    V1PersistentVolume replacePersistentVolume(String name, V1PersistentVolume persistentVolume, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询命名空间下的所有ConfigMap
     * @param namespace 指定的命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定命名空间下的所有ConfigMap
     * @throws ApiException
     */
    List<V1ConfigMap> listNamespacedConfigMap(String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定的ConfigMap信息
     * @param namespace 命名空间
     * @param name ConfigMap名
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定条件的ConfigMap信息
     * @throws ApiException 查询失败
     */
    V1ConfigMap readNamespacedConfigMap(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 创建指定的configMap
     * @param namespace 命名空间
     * @param configMap 需要被创建的configMap
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 创建的ConfigMap信息
     * @throws ApiException
     */
    V1ConfigMap createNamespacedConfigMap(String namespace, V1ConfigMap configMap, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 将指定的configMap替换成新的configMap
     * @param name 需要替换的configMap的名称
     * @param namespace 命名空间
     * @param configMap 替换后的configMap
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 替换结果
     * @throws ApiException
     */
    V1ConfigMap replaceNamespacedConfigMap(String name, String namespace, V1ConfigMap configMap, String k8sApiUrl, String authToken) throws ApiException;
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
     * 从nexus获取configMap的内容
     * @param namespace 命名空间
     * @param configMapName configMap的名字
     * @param cfgs 存在nexus上的配置文件
     * @param nexusHostUrl nexus的url
     * @param nexusUser nexus的登录用户
     * @param nexusPwd nexus的登录密码
     * @return 可以在k8s上部署的configMap
     * @throws IOException
     */
    V1ConfigMap getConfigMapFromNexus(String namespace, String configMapName, List<NexusAssetInfo> cfgs, String nexusHostUrl, String nexusUser, String nexusPwd) throws InterfaceCallException, IOException;

    /**
     * 删除已经存在的configMap
     * @param name configMap的名称
     * @param namespace 命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @throws ApiException
     */
    void deleteNamespacedConfigMap(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询命名空间下的所有Deployment
     * @param namespace 指定的命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定命名空间下的所有Deployment
     * @throws ApiException
     * @throws ApiException
     */
    List<V1Deployment> listNamespacedDeployment(String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定的Deployment信息
     * @param name Deployment名
     * @param namespace 命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定条件的Deployment信息
     * @throws ApiException 查询失败
     */
    V1Deployment readNamespacedDeployment(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定的Deployment的状态
     * @param name Deployment名
     * @param namespace 命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定条件的status信息
     * @throws ApiException 查询失败
     */
    K8sStatus readNamespacedDeploymentStatus(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 在指定的k8s系统里为指定命名空间创建deployment
     * @param namespace 需要创建deployment的命名空间
     * @param deployment 需要被创建的deployment
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 创建后的deployment
     * @throws ApiException
     */
    V1Deployment createNamespacedDeployment(String namespace, V1Deployment deployment, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 删除命名空间下的指定Deployment
     * @param namespace 命名空间
     * @param name 被删除的Deployment名称
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 删除结果
     * @throws ApiException
     */
    V1Status deleteNamespacedDeployment(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 删除命名空间下的所有deployment
     * @param namespace 命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 删除结果
     * @throws ApiException
     */
    V1Status deleteCollectionNamespacedDeployment(String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 替换已有的Deployment
     * @param name 被替换的Deployment的name
     * @param namespace 被替换的Deployment所属的命名空间
     * @param deployment 用来替换的Deployment
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 替换完成的Deployment
     * @throws ApiException
     */
    V1Deployment replaceNamespacedDeployment(String name, String namespace, V1Deployment deployment, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 选择指定命名空间下满足条件的所有deployment
     * @param namespace 命名空间
     * @param selector 选择条件
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 选择结果
     * @throws ApiException
     */
    List<V1Deployment> selectNamespacedDeployment(String namespace, Map<String, String> selector, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定的服务信息
     * @param name 服务名
     * @param namespace 命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定条件的服务信息
     * @throws ApiException 查询失败
     */
    V1Service readNamespacedService(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定命名空间下所有服务
     * @param namespace 指定的命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 该命名空间下所有服务
     * @throws ApiException 查询失败
     */
    List<V1Service> listNamespacedService(String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 在指定的k8s系统里为指定命名空间创建deployment
     * @param namespace 创建service的命名空间
     * @param service 需要被创建的service
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 创建后的deployment
     * @throws ApiException
     */
    V1Service createNamespacedService(String namespace, V1Service service, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 删除命名空间下的指定Service
     * @param namespace 命名空间
     * @param name 被删除的Service名称
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 删除结果
     * @throws ApiException
     */
    V1Status deleteNamespacedService(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 替换已有的Service
     * @param name 被替换的Service的name
     * @param namespace 被替换的Service所属的命名空间
     * @param service 用来替换的Service
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 替换完成的Service
     * @throws ApiException
     */
    V1Service replaceNamespacedService(String name, String namespace, V1Service service, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 选择指定命名空间下满足条件的所有service
     * @param namespace 命名空间
     * @param selector 选择条件
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 选择结果
     * @throws ApiException
     */
    List<V1Service> selectNamespacedService(String namespace, Map<String, String> selector, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定的命名空间
     * @param name 命名空间名称
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定名称的命名空间
     * @throws ApiException 调用k8s api时返回异常
     */
    V1Namespace readNamespace(String name, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询k8s上所有的命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return k8s上所有命名空间
     * @throws ApiException 调用k8s api时返回异常
     */
    List<V1Namespace> listNamespace(String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 创建指定的namespace
     * @param namespace 需要被创建的命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 创建后的deployment
     * @throws ApiException
     */
    V1Namespace createNamespace(V1Namespace namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 删除已有的命名空间
     * @param name 需要被删除的命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 删除结果
     * @throws ApiException
     */
    V1Status deleteNamespace(String name, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 创建一个缺省的命名空间
     * @param name 新建命名空间的名字
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 创建后的namespace
     * @throws ApiException
     */
    V1Namespace createDefaultNamespace(String name, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 检查指定的命名空间是否存在
     * @param name 指定命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 存在返回true，否则false
     * @throws ApiException
     */
    boolean isNamespaceExist(String name, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定命名空间下的所有Ingress
     * @param namespace 指定命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 查询结果
     * @throws ApiException
     */
    List<ExtensionsV1beta1Ingress> listNamespacedIngress(String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 查询指定的Ingress
     * @param namespace 命名空间
     * @param name ingress的名称
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 指定namespace的ingress信息
     * @throws ApiException
     */
    ExtensionsV1beta1Ingress readNamespacedIngress(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 在指定的k8s系统里为指定命名空间创建ingress
     * @param namespace 创建service的命名空间
     * @param ingress 需要被创建的ingress
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 创建后的deployment
     * @throws ApiException
     */
    ExtensionsV1beta1Ingress createNamespacedIngress(String namespace, ExtensionsV1beta1Ingress ingress, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 删除指定的Ingress
     * @param namespace 命名空间
     * @param name 被删除的Ingress名称
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 删除结果
     * @throws ApiException
     */
    V1Status deleteNamespacedIngress(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 替换已有的Ingress
     * @param name 被替换的Ingress的name
     * @param namespace 被替换的Ingress所属的命名空间
     * @param ingress 用来替换的Ingress
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 替换完成的Ingress
     * @throws ApiException
     */
    ExtensionsV1beta1Ingress replaceNamespacedIngress(String name, String namespace, ExtensionsV1beta1Ingress ingress, String k8sApiUrl, String authToken) throws ApiException;

    /**
     * 为指定的namespace创建ssl证书
     * @param namespace 命名空间
     * @param k8sApiUrl k8s的api的url
     * @param authToken 访问k8s api的认证token
     * @return 创建的命名空间
     * @throws ApiException
     */
    V1Secret createNamespacedSSLCert(String namespace, String k8sApiUrl, String authToken) throws ApiException;
}
