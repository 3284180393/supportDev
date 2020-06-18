package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.constant.PlatformFunction;
import com.channelsoft.ccod.support.cmdb.exception.*;
import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;
import com.channelsoft.ccod.support.cmdb.po.PlatformPo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformAppDeployDetailVo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformAppModuleVo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformTopologyInfo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformUpdateSchemaInfo;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;

import java.io.IOException;
import java.util.List;

/**
 * @ClassName: IPlatformManagerService
 * @Author: lanhb
 * @Description: 用来定义平台管理的接口类
 * @Date: 2020/5/28 9:53
 * @Version: 1.0
 */
public interface IPlatformManagerService {

    /**
     * 从k8s生成指定平台的拓扑结构
     * @param platformName 平台名
     * @param platformId 平台id
     * @param bkBizId 平台在蓝鲸paas的biz id
     * @param bkCloudId 平台服务器在蓝鲸paas的cloud id
     * @param ccodVersion 平台的ccod大版本号
     * @param k8sApiUrl k8s api的url
     * @param k8sAuthToken k8s api的访问token
     * @param func 平台用途
     * @return 平台拓扑
     * @throws ApiException
     * @throws ParamException
     * @throws NotSupportAppException
     * @throws NexusException
     * @throws LJPaasException
     * @throws InterfaceCallException
     * @throws IOException
     */
    PlatformTopologyInfo getPlatformTopologyFromK8s(String platformName, String platformId, int bkBizId, int bkCloudId, String ccodVersion, String k8sApiUrl, String k8sAuthToken, PlatformFunction func) throws ApiException, ParamException, NotSupportAppException, NexusException, LJPaasException, InterfaceCallException, IOException;

    /**
     * 删除指定平台的升级计划
     * @param platformId 指定的平台id
     * @throws ParamException 指定的平台不存在
     */
    void deletePlatformUpdateSchema(String platformId) throws ParamException;

    /**
     * 创建平台应用更新任务
     * @param platformId 平台id
     * @param platformName 平台名
     * @throws Exception 创建任务失败
     */
    void createNewPlatformAppDataUpdateTask(String platformId, String platformName) throws Exception;

    /**
     * 删除已有的平台
     * @param platformId 被删除的平台的id
     * @throws ParamException 被删除的平台不存在
     */
    void deletePlatform(String platformId) throws ParamException;

    /**
     * 检查指定条件的平台的应用部署情况，并上传对应的安装包和配置文件
     * @param platformId 平台id，不能为空
     * @param platformName 平台名,不能为空
     * @param bkBizId 该平台在蓝鲸paas上的的biz id
     * @param bkCloudId 该平台服务器在蓝鲸paas上的cloud id
     * @throws Exception
     */
    void startCollectPlatformAppData(String platformId, String platformName, int bkBizId, int bkCloudId) throws Exception;

    /**
     * 查询指定平台的命名空间
     * @param platformId 指定平台的id
     * @return 平台的的k8s的命名空间
     * @throws ParamException
     * @throws ApiException
     */
    V1Namespace queryPlatformK8sNamespace(String platformId) throws ParamException, ApiException;

    /**
     * 查询指定平台下的所有k8s pod信息
     * @param platformId 平台id
     * @return 平台下的所有pod信息
     * @throws ParamException
     * @throws ApiException
     */
    List<V1Pod> queryPlatformAllK8sPods(String platformId) throws ParamException, ApiException;

    /**
     * 查询指定平台下的指定pod名的pod信息
     * @param platformId 平台id
     * @param podName pod名
     * @return 查询到的指定条件的pod信息
     * @throws ParamException
     * @throws ApiException
     */
    V1Pod queryPlatformK8sPodByName(String platformId, String podName)throws ParamException, ApiException;

    /**
     * 查询指定平台下的所有k8s service信息
     * @param platformId 平台id
     * @return 平台下的所有service信息
     * @throws ParamException
     * @throws ApiException
     */
    List<V1Service> queryPlatformAllK8sServices(String platformId) throws ParamException, ApiException;

    /**
     * 查询指定平台下的指定pod名的pod信息
     * @param platformId 平台id
     * @param serviceName service名
     * @return 查询到的指定条件的pod信息
     * @throws ParamException
     * @throws ApiException
     */
    V1Service queryPlatformK8sServiceByName(String platformId, String serviceName)throws ParamException, ApiException;

    /**
     * 查询指定平台下的所有k8s configMap信息
     * @param platformId 平台id
     * @return 平台下的所有configMap信息
     * @throws ParamException
     * @throws ApiException
     */
    List<V1ConfigMap> queryPlatformAllK8sConfigMaps(String platformId) throws ParamException, ApiException;

    /**
     * 查询指定平台下的指定pod名的configMap信息
     * @param platformId 平台id
     * @param configMapName configMap名
     * @return 查询到的指定条件的pod信息
     * @throws ParamException
     * @throws ApiException
     */
    V1ConfigMap queryPlatformK8sConfigMapByName(String platformId, String configMapName)throws ParamException, ApiException;

    /**
     * 查询指定平台下的所有deployment
     * @param platformId 平台id
     * @return 平台下所有平台id
     * @throws ParamException
     * @throws ApiException
     */
    List<V1Deployment> queryPlatformAllK8sDeployment(String platformId) throws ParamException, ApiException;

    /**
     * 查询指定平台下的指定名的deployment
     * @param platformId 平台id
     * @param deploymentName 指定的deployment名
     * @return 满足指定条件的deployment
     */
    V1Deployment queryPlatformK8sDeploymentByName(String platformId, String deploymentName) throws ParamException, ApiException;

    /**
     * 查询平台指定名的k8s ingress信息
     * @param platformId 平台id
     * @param ingressName k8s ingress名
     * @return 查询结果
     * @throws ParamException
     * @throws ApiException
     */
    ExtensionsV1beta1Ingress queryPlatformK8sIngressByName(String platformId, String ingressName) throws ParamException, ApiException;

    /**
     * 查询平台所有k8s ingress信息
     * @param platformId 平台id
     * @return 查询结果
     * @throws ParamException
     * @throws ApiException
     */
    List<ExtensionsV1beta1Ingress> queryPlatformAllK8sIngress(String platformId) throws ParamException, ApiException;

    /**
     * 查询平台所有endpoints
     * @param platformId 指定平台id
     * @return 指定平台的所有k8s endpoints
     * @throws ParamException
     * @throws ApiException
     */
    List<V1Endpoints> queryPlatformAllK8sEndpoints(String platformId) throws ParamException, ApiException;

    /**
     * 查询指定平台的指定名称的k8s endpoints
     * @param platformId 平台id
     * @param endpointsName  k8s endpoints名称
     * @return 查询结果
     * @throws ParamException
     * @throws ApiException
     */
    V1Endpoints queryPlatformK8sEndpointsByName(String platformId, String endpointsName) throws ParamException, ApiException;

    /**
     * 查询平台所有Secret
     * @param platformId 指定平台id
     * @return 指定平台的所有k8s Secret
     * @throws ParamException
     * @throws ApiException
     */
    List<V1Secret> queryPlatformAllK8sSecret(String platformId) throws ParamException, ApiException;

    /**
     * 查询指定平台的指定名称的k8s Secret
     * @param platformId 平台id
     * @param secretName  k8s Secret名称
     * @return 查询结果
     * @throws ParamException
     * @throws ApiException
     */
    V1Secret queryPlatformK8sSecretByName(String platformId, String secretName) throws ParamException, ApiException;

    /**
     * 查询平台所有PersistentVolumeClaim
     * @param platformId 指定平台id
     * @return 指定平台的所有k8s PersistentVolumeClaim
     * @throws ParamException
     * @throws ApiException
     */
    List<V1PersistentVolumeClaim> queryPlatformAllK8sPersistentVolumeClaim(String platformId) throws ParamException, ApiException;

    /**
     * 查询指定平台的指定名称的k8s PersistentVolumeClaim
     * @param platformId 平台id
     * @param persistentVolumeClaimName  k8s PersistentVolumeClaim名称
     * @return 查询结果
     * @throws ParamException
     * @throws ApiException
     */
    V1PersistentVolumeClaim queryPlatformK8sPersistentVolumeClaimByName(String platformId, String persistentVolumeClaimName) throws ParamException, ApiException;

    /**
     * 为新建平台创建configMap
     * @param createSchema 新建平台schema
     * @return 为新平台创建的configMap
     * @throws ParamException
     * @throws InterfaceCallException
     * @throws NexusException
     */
    List<V1ConfigMap> createConfigMapForNewPlatform(PlatformUpdateSchemaInfo createSchema) throws InterfaceCallException, IOException, ApiException;


//    List<PlatformAppDeployDetailVo> updatePlatformAppTopologyFromK8s(String platformId) throws ApiException;
}
