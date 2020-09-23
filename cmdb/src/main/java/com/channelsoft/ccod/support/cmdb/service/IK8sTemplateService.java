package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.constant.K8sKind;
import com.channelsoft.ccod.support.cmdb.constant.ServicePortType;
import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;
import com.channelsoft.ccod.support.cmdb.k8s.vo.K8sCCODDomainAppVo;
import com.channelsoft.ccod.support.cmdb.k8s.vo.K8sThreePartAppVo;
import com.channelsoft.ccod.support.cmdb.k8s.vo.K8sThreePartServiceVo;
import com.channelsoft.ccod.support.cmdb.po.AppBase;
import com.channelsoft.ccod.support.cmdb.po.PlatformPo;
import com.channelsoft.ccod.support.cmdb.vo.AppFileNexusInfo;
import com.channelsoft.ccod.support.cmdb.vo.AppUpdateOperationInfo;
import com.channelsoft.ccod.support.cmdb.vo.K8sOperationInfo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformAppDeployDetailVo;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: IK8sTemplateService
 * @Author: lanhb
 * @Description: 用来管理k8s资源对象模板的服务接口
 * @Date: 2020/8/6 9:45
 * @Version: 1.0
 */
public interface IK8sTemplateService {

    ExtensionsV1beta1Ingress generateIngress(String ccodVersion, AppType appType, String appName, String alias, String platformId, String domainId, String hostUrl) throws ParamException;

    V1Service generateCCODDomainAppService(String ccodVersion, AppType appType, String appName, String alias, ServicePortType portType, String portStr, String platformId, String domainId) throws ParamException;

    V1Service generateThreeAppService(String ccodVersion, String appName, String alias, String version, String platformId) throws ParamException;

    /**
     * 为ccod域应用生成deployment
     * @param appBase 域应用相关基础信息
     * @param domainId 域id
     * @param domainCfg 域公共配置
     * @param platform k8s平台信息
     * @return 生成的模板
     * @throws ParamException
     */
    V1Deployment generateCCODDomainAppDeployment(AppBase appBase, String domainId, List<AppFileNexusInfo> domainCfg, PlatformPo platform) throws ParamException;

    /**
     * 生成第三方应用的deployment
     * @param ccodVersion 平台的ccod大版本
     * @param appName 第三方应用名
     * @param alias 第三方应用别名
     * @param version 版本号
     * @param platformId 平台id
     * @param hostUrl 平台的访问域名
     * @return 第三方应用的deployment
     */
    V1Deployment generateThreeAppDeployment(String ccodVersion, String appName, String alias, String version, String platformId, String hostUrl) throws ParamException;

    V1Namespace generateNamespace(String ccodVersion, String platformId) throws ParamException;

    V1Secret generateSecret(String ccodVersion, String platformId, String name) throws ParamException;

    V1PersistentVolume generatePersistentVolume(String ccodVersion, String platformId) throws ParamException;

    V1PersistentVolumeClaim generatePersistentVolumeClaim(String ccodVersion, String platformId) throws ParamException;

    /**
     * 生成平台初始job
     * @param platform 需要生成初始job的平台
     * @return 平台初始job
     * @throws ParamException
     */
    V1Job generatePlatformInitJob(PlatformPo platform) throws ParamException;

    /**
     * 获取已经部署的ccod域应用在k8s上的部署详情
     * @param appName 应用名
     * @param alias 别名
     * @param version 版本
     * @param domainId 域id
     * @param platformId 平台id
     * @param k8sApiUrl k8s的api的url
     * @param k8sAuthToken 访问k8s api的认证token
     * @return ccod域应用在k8s上的部署详情
     * @throws ParamException
     * @throws ApiException
     */
    K8sCCODDomainAppVo getCCODDomainApp(String appName, String alias, String version, String domainId, String platformId,
                                        String k8sApiUrl, String k8sAuthToken) throws ParamException, ApiException;

    /**
     * 获取新加入的/被修改的ccod域应用的k8s资源信息
     * @param appBase 应用基础信息
     * @param domainId 域id
     * @param domainCfg 域公共配置
     * @param platform k8s平台相关信息
     * @return ccod域应用的k8s资源信息
     * @throws ParamException
     */
    K8sCCODDomainAppVo generateNewCCODDomainApp(AppBase appBase, String domainId, List<AppFileNexusInfo> domainCfg, PlatformPo platform) throws ParamException, InterfaceCallException, IOException;

    /**
     * 预检查对已经存在域进行增删改以及调试操作k8s是否可以执行（例如命名是否冲突、新添的已经存在、被删除或是修改调试的不存在等）
     * @param domainId 域id
     * @param optList 需要执行的域应用相关操作
     * @param platformId 平台id
     * @param k8sApiUrl k8s的api的url
     * @param k8sAuthToken 访问k8s api的认证token
     * @return 域检查结果，如果检查通过返回"",否则返回检查失败描述
     * @throws ApiException
     */
    String preCheckCCODDomainApps(String domainId, List<AppUpdateOperationInfo> optList, String platformId,
                                  String k8sApiUrl, String k8sAuthToken) throws ApiException;

    /**
     * 生成删除已有的域应用的k8s操作步骤
     * @param jobId 任务的job id
     * @param appName 应用名
     * @param alias 别名
     * @param version 版本
     * @param domainId 域id
     * @param platformId 平台id
     * @param k8sApiUrl k8s的api的url
     * @param k8sAuthToken 访问k8s api的认证token
     * @return 删除该应用需要执行的k8s步骤
     * @throws ApiException
     * @throws ParamException
     */
    List<K8sOperationInfo> getDeletePlatformAppSteps(String jobId, String appName, String alias, String version,
                                                     String domainId, String platformId, String k8sApiUrl,
                                                     String k8sAuthToken)
            throws ApiException, ParamException;

    /**
     * 生成添加新ccod域应用的k8s操作步骤
     * @param jobId 任务id
     * @param appBase 需要添加的域应用相关信息
     * @param domainId 域id
     * @param domainCfg 域公共配置
     * @param platform k8s平台相关信息
     * @param isNewPlatform 该平台是否为新建平台
     * @return 添加域应用的k8s操作步骤
     * @throws ParamException
     * @throws ApiException
     */
    List<K8sOperationInfo> generateAddPlatformAppSteps(String jobId, AppBase appBase, String domainId, List<AppFileNexusInfo> domainCfg, PlatformPo platform, boolean isNewPlatform) throws ParamException, ApiException, InterfaceCallException, IOException;

    /**
     * 生成修改ccod域应用的k8s操作步骤
     * @param jobId 任务id
     * @param appBase 应用相关基础信息
     * @param domainId 域id
     * @param domainCfg 域公共配置
     * @param platform k8s平台信息
     * @return 修改域应用的k8s操作步骤
     * @throws ParamException
     * @throws ApiException
     */
    List<K8sOperationInfo> generateUpdatePlatformAppSteps(String jobId, AppBase appBase, String domainId, List<AppFileNexusInfo> domainCfg, PlatformPo platform) throws ParamException, ApiException, InterfaceCallException, IOException;

    /**
     * 生成修改ccod域应用的k8s调试步骤
     * @param jobId 任务id
     * @param appBase 应用相关基础信息
     * @param domainId 域id
     * @param domainCfg 域公共配置
     * @param platform k8s平台相关信息
     * @return 修改域应用的k8s调试步骤
     * @throws ParamException
     * @throws ApiException
     */
    List<K8sOperationInfo> generateDebugPlatformAppSteps(String jobId, AppBase appBase, String domainId, List<AppFileNexusInfo> domainCfg, PlatformPo platform) throws ParamException, ApiException, InterfaceCallException, IOException;

    /**
     * 生成选择器用于选择k8s上的ccod域应用相关资源
     * @param appName 应用名
     * @param alias 应用别名
     * @param version 应用版本
     * @param appType 应用类型
     * @param domainId 域id
     * @param kind k8s资源类型
     * @return 生成的选择器
     */
    Map<String, String> getCCODDomainAppSelector(String appName, String alias, String version, AppType appType, String domainId, K8sKind kind);

    /**
     * 生成指定条件的k8s 模板选择器
     * @param ccodVersion ccod大版本号
     * @param appName 应用名
     * @param vesion 版本
     * @param appType 应用类型
     * @param kind 希望选择的k8s资源类型
     * @return 指定条件的模板选择器
     */
    Map<String, String> getK8sTemplateSelector(String ccodVersion, String appName, String vesion, AppType appType, K8sKind kind);

    /**
     * 生成平台创建步骤
     * @param jobId 创建平台的任务id
     * @param job 创建平台需要预执行的job
     * @param namespace 创建平台的namespace信息，如果为空将根据现有模板自动创建
     * @param secrets 平台的相关secret，如果为空将自动创建ssl cert
     * @param pv 平台使用的pv,如果为空将通过模板自动创建
     * @param pvc 平台使用的pvc,如果为空将通过模板自动创建
     * @param threePartApps 平台依赖的第三方应用，如果为空将根据模板自动创建oracle和mysql
     * @param threePartServices 平台依赖的第三方服务，如果为空将根据模板自动创建umg141,umg147和umg41三个缺省第三方服务
     * @param platform 需要被创建的平台
     * @return 平台创建所需执行的步骤
     * @throws ApiException
     * @throws ParamException
     * @throws IOException
     * @throws InterfaceCallException
     */
    List<K8sOperationInfo> generatePlatformCreateSteps(String jobId, V1Job job, V1Namespace namespace, List<V1Secret> secrets, V1PersistentVolume pv, V1PersistentVolumeClaim pvc, List<K8sThreePartAppVo> threePartApps, List<K8sThreePartServiceVo> threePartServices, PlatformPo platform) throws ApiException, ParamException, IOException, InterfaceCallException;

    /**
     * 生成一组用于测试的第三方服务
     * @param ccodVersion 平台的ccod大版本
     * @param platformId 平台id
     * @return 用于测试的第三方服务
     * @throws ApiException
     * @throws ParamException
     */
    List<K8sThreePartServiceVo> generateTestThreePartServices(String ccodVersion, String platformId) throws ApiException, ParamException;

    /**
     * 查询指定平台所有ccod模块在k8s上的运行状态
     * @param platform 指定的基于k8s的平台
     * @return 所有ccod模块在k8s上运行状态
     * @throws ApiException
     */
    List<PlatformAppDeployDetailVo> getPlatformAppDetailFromK8s(PlatformPo platform) throws ApiException;

    /**
     * 查询指定ccod模块在k8s上的运行状态
     * @param platform 平台信息
     * @param domainId 域id
     * @param alias 应用别名
     * @return 指定ccod模块在k8s上的运行状态
     * @throws ApiException
     */
    PlatformAppDeployDetailVo getPlatformAppDetailFromK8s(PlatformPo platform, String domainId, String alias) throws ApiException;

}
