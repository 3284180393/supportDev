package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.config.BizSetDefine;
import com.channelsoft.ccod.support.cmdb.constant.AppUpdateOperation;
import com.channelsoft.ccod.support.cmdb.constant.PlatformDeployStatus;
import com.channelsoft.ccod.support.cmdb.constant.PlatformFunction;
import com.channelsoft.ccod.support.cmdb.exception.*;
import com.channelsoft.ccod.support.cmdb.po.*;
import com.channelsoft.ccod.support.cmdb.vo.*;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;

import java.io.IOException;
import java.sql.SQLException;
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
     * @param hostIp k8s服务器ip
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
    PlatformTopologyInfo getPlatformTopologyFromK8s(String platformName, String platformId, int bkBizId, int bkCloudId, String ccodVersion, String hostIp, String k8sApiUrl, String k8sAuthToken, PlatformFunction func) throws ApiException, ParamException, NotSupportAppException, NexusException, LJPaasException, InterfaceCallException, IOException, K8sDataException;

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
     * 查询指定id的平台
     * @param platformId 平台id
     * @return 查询到的平台记录
     * @throws ParamException
     */
    PlatformPo queryPlatformById(String platformId) throws ParamException;

    /**
     * 查询所有的平台记录
     * @return 查询到的所有平台记录
     */
    List<PlatformPo> queryAllPlatform();

    /**
     * 查询指定平台下的指定id的域
     * @param domainId 域id
     * @param platformId 平台id
     * @return 查询结果
     * @throws ParamException
     */
    DomainPo queryDomainById(String domainId, String platformId) throws ParamException;

    /**
     * 查询指定平台下的所有域
     * @param platformId
     * @return 指定平台下的所有域
     * @throws ParamException
     */
    List<DomainPo> queryAllDomain(String platformId) throws ParamException;

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
     * 开始收集平台更新数据
     * @param platformId
     * @param platformName
     * @return 收集结果
     * @throws Exception
     */
    PlatformAppModuleVo[] startCollectPlatformAppUpdateData(String platformId, String platformName) throws Exception;

    /**
     * 更新已有的平台应用模块
     * @param platformId 平台id
     * @param platformName 平台名
     * @param appList 需要更新的平台应用列表
     * @return 更新后的平台应用信息
     * @throws ParamException 需要更新的平台应用信息错误
     * @throws InterfaceCallException 调用接口发生异常
     * @throws NexusException 调用nexus的api返回调用错误或是解析nexus返回结果异常
     */
    List<PlatformAppPo> updatePlatformApps(String platformId, String platformName, List<AppUpdateOperationInfo> appList) throws NotSupportAppException, ParamException, InterfaceCallException, NexusException, LJPaasException, IOException;

    /**
     * 查询指定条件的平台升级计划
     * @param platformId 平台id可以为空
     * @return 满足条记按的升级计划
     */
    List<PlatformUpdateSchemaInfo> queryPlatformUpdateSchema(String platformId);


    /**
     * 查询指定平台的拓扑接口
     * @param platformId 平台id
     * @return 平台的拓扑
     * @throws ParamException 计划的参数异常
     * @throws InterfaceCallException 处理计划时调用蓝鲸api或是nexus api失败
     * @throws LJPaasException 调用蓝鲸api返回调用失败或是解析蓝鲸api结果失败
     * @throws NexusException 调用nexus api返回调用失败或是解析nexus api返回结果失败
     */
    PlatformTopologyInfo getPlatformTopology(String platformId) throws ParamException, InterfaceCallException, LJPaasException, NotSupportAppException;

    /**
     * 查询所有平台简单拓扑结构
     * @return 当前所有平台的状态
     * @throws ParamException
     * @throws InterfaceCallException
     * @throws LJPaasException
     * @throws NotSupportAppException
     */
    List<PlatformTopologyInfo> queryAllPlatformTopology() throws ParamException, InterfaceCallException, LJPaasException, NotSupportAppException;

    /**
     * 查询指定模块的部署状态信息
     * @param platformId 平台id
     * @param isGetCfg 是否获取平台配置
     * @return 已经部署模块的状态明细
     * @throws ApiException
     */
    List<PlatformAppDeployDetailVo> queryPlatformCCODAppDeployStatus(String platformId, boolean isGetCfg) throws ApiException, ParamException, IOException, InterfaceCallException, NexusException;


    /**
     * 查询平台指定模块的k8s部署状态
     * @param platformId 平台id
     * @param domainId 域id
     * @param appName 应用名
     * @param alias 别名
     * @param isGetCfg 是否获取应用配置文件
     * @return 指定模块在k8s上的状态
     * @throws ApiException
     */
    PlatformAppDeployDetailVo queryPlatformCCODAppDeployStatus(String platformId, String domainId, String appName, String alias, boolean isGetCfg) throws ApiException, ParamException, IOException, InterfaceCallException, NexusException;

    /**
     * 查询平台正在进行调试的应用
     * @param platformId 平台id
     * @return 平台正在进行调试的应用
     */
    List<AppDebugDetailPo> queryPlatformDebugApps(String platformId);

    /**
     * 更新平台升级计划
     * @param updateSchema 需要更新的平台计划
     * @throws ParamException 计划的参数异常
     * @throws InterfaceCallException 处理计划时调用蓝鲸api或是nexus api失败
     * @throws LJPaasException 调用蓝鲸api返回调用失败或是解析蓝鲸api结果失败
     * @throws NexusException 调用nexus api返回调用失败或是解析nexus api返回结果失败
     * @throws IOException 处理文件失败
     */
    void updatePlatformUpdateSchema(PlatformUpdateSchemaInfo updateSchema) throws NotSupportSetException, NotSupportAppException, ParamException, InterfaceCallException, LJPaasException, NexusException, IOException, ApiException, K8sDataException, ClassNotFoundException, SQLException;

    /**
     * 查询平台回滚信息
     * @param platformId 需要回滚的平台id
     * @return 平台回滚信息
     * @throws ParamException
     */
    List<DomainUpdatePlanInfo> queryPlatformRollbackInfo(String platformId) throws ParamException;

    /**
     * 查询所有的平台升级记录
     * @return 查询结果
     */
    List<PlatformUpdateRecordVo> queryPlatformUpdateRecords();

    /**
     * 查询指定平台升级记录
     * @param platformId 平台id
     * @return 查询结果
     * @throws ParamException
     */
    List<PlatformUpdateRecordVo> queryPlatformUpdateRecordByPlatformId(String platformId) throws ParamException;

    /**
     * 查询指定job id的平台升级记录
     * @param platformId 平台id
     * @param jobId job id
     * @return 查询结果
     * @throws ParamException platformId或是job不存在或是jobId和platformId不一致
     */
    PlatformUpdateRecordVo queryPlatformUpdateRecordByJobId(String platformId, String jobId) throws ParamException;

    /**
     * 回滚指定的平台
     * @param platformId 平台id
     * @param domainIds 需要回滚的域
     * @return 回滚过程
     * @throws ParamException
     */
    PlatformUpdateRecordVo rollbackPlatform(String platformId, List<String> domainIds) throws ParamException, ApiException;

    /**
     * 创建新的升级计划
     * @param paramVo 被创建的平台相关参数
     * @return 新建的平台创建计划
     * @throws ParamException
     * @throws InterfaceCallException
     * @throws LJPaasException
     */
    PlatformUpdateSchemaInfo createNewPlatform(PlatformCreateParamVo paramVo) throws ParamException, NotSupportSetException, NotSupportAppException, InterfaceCallException, LJPaasException;

    /**
     * 生成平台创建脚本
     * @param schema 用来生成脚本的schema
     * @return 生成的脚本存放路径
     * @throws ParamException
     * @throws NexusException
     * @throws NotSupportAppException
     * @throws InterfaceCallException
     * @throws LJPaasException
     */
    String generatePlatformCreateScript(PlatformUpdateSchemaInfo schema) throws ParamException, NexusException, NotSupportAppException, InterfaceCallException, LJPaasException, IOException, ApiException;

    /**
     * 为脚本部署生成schema
     * @param paramVo 通过脚本部署的平台相关参数
     * @return 生成的schema
     */
    PlatformUpdateSchemaInfo generateSchemaForScriptDeploy(PlatformCreateParamVo paramVo);

    /**
     * 通过脚本在主机上部署规划平台
     * @param schema 规划好的平台
     * @throws NotSupportSetException
     * @throws NotSupportAppException
     * @throws ParamException
     * @throws InterfaceCallException
     * @throws LJPaasException
     * @throws NexusException
     * @throws IOException
     * @throws ApiException
     * @throws K8sDataException
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    void deployPlatformByHostScript(PlatformUpdateSchemaInfo schema) throws NotSupportSetException, NotSupportAppException, ParamException, InterfaceCallException, LJPaasException, NexusException, IOException, ApiException, K8sDataException, ClassNotFoundException, SQLException;

    /**
     * 是否平台部署正在进行
     * @param platformId 平台id
     * @return true正在进行，false已经结束
     */
    boolean isPlatformDeployOngoing(String platformId);

    /**
     * 获取平台部署日志
     * @return 平台部署日志
     */
    List<K8sOperationPo> getPlatformDeployLogs();

    List<K8sOperationPo> getAppDebugLogs(String platformId, String domainId, String appName, String alias);

    /**
     * 获取最后一次平台部署任务状态
     * @return 部署状态
     */
    PlatformDeployStatus getLastPlatformDeployTaskStatus();

    /**
     * 根据域id获取域归属的set信息
     * @param domainId 域id
     * @return 归属的set信息
     * @throws ParamException
     */
    BizSetDefine getBizSetForDomainId(String domainId) throws ParamException;

    /**
     * 查询指定应用模块的配置文件
     * @param platformId 平台id
     * @param domainId 域id
     * @param alias 应用别名
     * @return
     * @throws ParamException
     */
    List<AppFileNexusInfo> queryPlatformAppCfgs(String platformId, String domainId, String alias) throws ParamException;

    /**
     * 查询指定的平台应用详情
     * @param platformId 平台id
     * @param domainId 域id
     * @param alias 别名
     * @return 指定详情
     */
    PlatformAppDeployDetailVo queryPlatformApp(String platformId, String domainId, String alias);

    /**
     * 调试平台应用
     * @param platformId 平台id
     * @param domainId 域id
     * @param optInfo 需要调试的相关内容
     */
    void debugPlatformApp(String platformId, String domainId, AppUpdateOperationInfo optInfo) throws ParamException, InterfaceCallException, LJPaasException, ApiException;

    /**
     * 定时处理ccod应用模块调试
     */
    void debugHandle();

    /**
     * 获取指定版本ccod平台所依赖的第三方应用
     * @param ccodVersion ccod大版本号
     * @param tag 用来细分指定大版本平台的标签
     * @return 指定条件的记录
     */
    List<CCODThreePartAppPo> getThreePartAppDepend(String ccodVersion, String tag);

    /**
     * 从k8s恢复出平台拓扑
     * @param platformId 平台id
     * @param platformName 平台名
     * @param ccodVersion ccod大版本号
     * @param k8sApiUrl k8s的api url
     * @param k8sAuthToken k8s的认证token
     * @return
     * @throws ApiException
     * @throws ParamException
     */
    PlatformTopologyInfo restorePlatformTopologyFromK8s(String platformId, String platformName, String ccodVersion, String k8sApiUrl, String k8sAuthToken) throws ApiException, ParamException;

    /**
     * 恢复已经存在的k8s平台
     * @param platformId 被恢复的k8s平台
     * @return 创建该平台的schema
     * @throws ParamException
     * @throws NotSupportSetException
     * @throws NotSupportAppException
     * @throws InterfaceCallException
     * @throws LJPaasException
     */
    PlatformUpdateSchemaInfo restoreExistK8sPlatform(String platformId) throws ParamException, InterfaceCallException, IOException, ApiException;

    /**
     * 从k8s删除指定平台的namespace
     * @param platformId 指定平台id
     * @throws ApiException
     */
    void deleteNamespaceFromK8s(String platformId) throws ApiException;

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
     * 为指定的平台创建 k8s Service
     * @param platformId 平台id
     * @param service 需要创建的SService定义
     * @return 被创建的Service
     * @throws ParamException 平台不存在或是平台的类型不是K8S_CONTAINER
     * @throws ApiException 调用k8s api异常
     */
    V1Service createK8sPlatformService(String platformId, V1Service service) throws ParamException, ApiException;

    /**
     * 为平台删除指定的Service
     * @param platformId 平台id
     * @param serviceName 被删除的Service的名称
     * @throws ParamException 平台不存在或是平台的类型不是K8S_CONTAINER
     * @throws ApiException 调用k8s api异常
     */
    void deleteK8sPlatformService(String platformId, String serviceName) throws ParamException, ApiException;

    /**
     * 替换平台已有的Service
     * @param platformId 平台id
     * @param serviceName 被替换的Service名称
     * @param service 新Service的定义
     * @return 被替换后的Service详细信息
     * @throws ParamException 平台不存在或是平台的类型不是K8S_CONTAINER
     * @throws ApiException 调用k8s api异常
     */
    V1Service replaceK8sPlatformService(String platformId, String serviceName, V1Service service) throws ParamException, ApiException;

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
     * 为指定的平台创建 k8s Deployment
     * @param platformId 平台id
     * @param deployment 需要创建的Deployment定义
     * @return 被创建的Deployment
     * @throws ParamException 平台不存在或是平台的类型不是K8S_CONTAINER
     * @throws ApiException 调用k8s api异常
     */
    V1Deployment createK8sPlatformDeployment(String platformId, V1Deployment deployment) throws ParamException, ApiException;

    /**
     * 为平台删除指定的Deployment
     * @param platformId 平台id
     * @param deploymentName 被删除的Deployment的名称
     * @throws ParamException 平台不存在或是平台的类型不是K8S_CONTAINER
     * @throws ApiException 调用k8s api异常
     */
    void deleteK8sPlatformDeployment(String platformId, String deploymentName) throws ParamException, ApiException;

    /**
     * 替换平台已有的Deployment
     * @param platformId 平台id
     * @param deploymentName 被替换的Deployment名称
     * @param deployment 新Deployment的定义
     * @return 被替换后的Deployment详细信息
     * @throws ParamException 平台不存在或是平台的类型不是K8S_CONTAINER
     * @throws ApiException 调用k8s api异常
     */
    V1Deployment replaceK8sPlatformDeployment(String platformId, String deploymentName, V1Deployment deployment) throws ParamException, ApiException;

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
     * 为指定的平台创建 k8s Ingress
     * @param platformId 平台id
     * @param ingress 需要创建的Ingress定义
     * @return 被创建的Ingress
     * @throws ParamException 平台不存在或是平台的类型不是K8S_CONTAINER
     * @throws ApiException 调用k8s api异常
     */
    ExtensionsV1beta1Ingress createK8sPlatformIngress(String platformId, ExtensionsV1beta1Ingress ingress) throws ParamException, ApiException;

    /**
     * 为平台删除指定的Ingress
     * @param platformId 平台id
     * @param ingressName 被删除的Ingress的名称
     * @throws ParamException 平台不存在或是平台的类型不是K8S_CONTAINER
     * @throws ApiException 调用k8s api异常
     */
    void deleteK8sPlatformIngress(String platformId, String ingressName) throws ParamException, ApiException;

    /**
     * 替换平台已有的Ingress
     * @param platformId 平台id
     * @param ingressName 被替换的Ingress名称
     * @param ingress 新Ingress的定义
     * @return 被替换后的Ingress详细信息
     * @throws ParamException 平台不存在或是平台的类型不是K8S_CONTAINER
     * @throws ApiException 调用k8s api异常
     */
    ExtensionsV1beta1Ingress replaceK8sPlatformIngress(String platformId, String ingressName, ExtensionsV1beta1Ingress ingress) throws ParamException, ApiException;

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
     * 为指定的平台创建 k8s Endpoints
     * @param platformId 平台id
     * @param endpoints 需要创建的Endpoints定义
     * @return 被创建的Endpoints
     * @throws ParamException 平台不存在或是平台的类型不是K8S_CONTAINER
     * @throws ApiException 调用k8s api异常
     */
    V1Endpoints createK8sPlatformEndpoints(String platformId, V1Endpoints endpoints) throws ParamException, ApiException;

    /**
     * 为平台删除指定的Endpoints
     * @param platformId 平台id
     * @param endpointsName 被删除的Endpoints的名称
     * @throws ParamException 平台不存在或是平台的类型不是K8S_CONTAINER
     * @throws ApiException 调用k8s api异常
     */
    void deleteK8sPlatformEndpoints(String platformId, String endpointsName) throws ParamException, ApiException;

    /**
     * 替换平台已有的Endpoints
     * @param platformId 平台id
     * @param endpointName 被替换的Endpoints名称
     * @param endpoints 新Endpoints的定义
     * @return 被替换后的Endpoints详细信息
     * @throws ParamException 平台不存在或是平台的类型不是K8S_CONTAINER
     * @throws ApiException 调用k8s api异常
     */
    V1Endpoints replaceK8sPlatformEndpoints(String platformId, String endpointName, V1Endpoints endpoints) throws ParamException, ApiException;

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
     * 为指定的平台创建 k8s Secret
     * @param platformId 平台id
     * @param secret 需要创建的Secret定义
     * @return 被创建的Secret
     * @throws ParamException 平台不存在或是平台的类型不是K8S_CONTAINER
     * @throws ApiException 调用k8s api异常
     */
    V1Secret createK8sPlatformSecret(String platformId, V1Secret secret) throws ParamException, ApiException;

    /**
     * 为平台删除指定的Secret
     * @param platformId 平台id
     * @param secretName 被删除的Secret的名称
     * @throws ParamException 平台不存在或是平台的类型不是K8S_CONTAINER
     * @throws ApiException 调用k8s api异常
     */
    void deleteK8sPlatformSecret(String platformId, String secretName) throws ParamException, ApiException;

    /**
     * 替换平台已有的Secret
     * @param platformId 平台id
     * @param secretName 被替换的Secret名称
     * @param secret 新Secret的定义
     * @return 被替换后的Secret详细信息
     * @throws ParamException 平台不存在或是平台的类型不是K8S_CONTAINER
     * @throws ApiException 调用k8s api异常
     */
    V1Secret replaceK8sPlatformSecret(String platformId, String secretName, V1Secret secret) throws ParamException, ApiException;

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
     * 为指定的平台创建 k8s PersistentVolumeClaim
     * @param platformId 平台id
     * @param persistentVolumeClaim 需要创建的PersistentVolumeClaim定义
     * @return 被创建的PersistentVolumeClaim
     * @throws ParamException 平台不存在或是平台的类型不是K8S_CONTAINER
     * @throws ApiException 调用k8s api异常
     */
    V1PersistentVolumeClaim createK8sPlatformPersistentVolumeClaim(String platformId, V1PersistentVolumeClaim persistentVolumeClaim) throws ParamException, ApiException;

    /**
     * 为平台删除指定的PersistentVolumeClaim
     * @param platformId 平台id
     * @param persistentVolumeClaimName 被删除的PersistentVolumeClaim的名称
     * @throws ParamException 平台不存在或是平台的类型不是K8S_CONTAINER
     * @throws ApiException 调用k8s api异常
     */
    void deleteK8sPlatformPersistentVolumeClaim(String platformId, String persistentVolumeClaimName) throws ParamException, ApiException;

    /**
     * 替换平台已有的PersistentVolumeClaim
     * @param platformId 平台id
     * @param persistentVolumeClaimName 被替换的PersistentVolumeClaim名称
     * @param persistentVolumeClaim 新PersistentVolumeClaim的定义
     * @return 被替换后的PersistentVolumeClaim详细信息
     * @throws ParamException 平台不存在或是平台的类型不是K8S_CONTAINER
     * @throws ApiException 调用k8s api异常
     */
    V1PersistentVolumeClaim replaceK8sPlatformPersistentVolumeClaim(String platformId, String persistentVolumeClaimName, V1PersistentVolumeClaim persistentVolumeClaim) throws ParamException, ApiException;

}
