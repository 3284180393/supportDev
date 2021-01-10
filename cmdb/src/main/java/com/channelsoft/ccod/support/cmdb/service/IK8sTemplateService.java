package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.config.BizSetDefine;
import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.constant.K8sKind;
import com.channelsoft.ccod.support.cmdb.constant.ServicePortType;
import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;
import com.channelsoft.ccod.support.cmdb.exception.NexusException;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;
import com.channelsoft.ccod.support.cmdb.k8s.vo.K8sCCODDomainAppVo;
import com.channelsoft.ccod.support.cmdb.k8s.vo.K8sThreePartServiceVo;
import com.channelsoft.ccod.support.cmdb.po.*;
import com.channelsoft.ccod.support.cmdb.vo.*;
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

    /**
     * 获取当前的k8s对象模板
     * @return k8s对象模板
     */
    List<K8sObjectTemplatePo> getK8sTemplates();

    /**
     * 为指定应用生成ingress
     * @param appBase 指定应用
     * @param domain 应用归属域
     * @param platform 平台信息
     * @return
     * @throws ParamException
     */
    List<ExtensionsV1beta1Ingress> generateIngress(AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform) throws ParamException;

    /**
     * 生成指定的域应用服务
     * @param appBase 域应用信息
     * @param portType 服务端口类型
     * @param portStr 端口字符串
     * @param domain 域信息
     * @param platform 平台信息
     * @return 生成的域服务
     * @throws ParamException
     */
    V1Service generateCCODDomainAppService(AppUpdateOperationInfo appBase, ServicePortType portType, String portStr, DomainPo domain, PlatformPo platform) throws ParamException;


    /**
     * 为ccod域应用生成deployment
     * @param opt 应用相关信息
     * @param domain 域定义
     * @param platform k8s平台信息
     * @return 生成的模板
     * @throws ParamException
     */
    V1Deployment generateCCODDomainAppDeployment(AppUpdateOperationInfo opt, DomainPo domain, PlatformPo platform) throws ParamException;

    /**
     * 生成第三方应用的deployment
     * @param threePartAppPo 需要生成deployment的第三方应用
     * @param platform 平台基本信息
     * @param isBase 第三方应用是否和ccod模块部署在同一个命名空间，true是，false否，第三方应用将会被部署在base-platform.platformId命名空间里
     * @return 第三方应用的deployment
     */
    List<V1Deployment> generateThreeAppDeployment(CCODThreePartAppPo threePartAppPo, PlatformPo platform, boolean isBase) throws ParamException;

    /**
     * 为指定的平台生成namespace
     * @param ccodVersion 平台的ccodVersion
     * @param platformId 平台id
     * @param platformTag 平台标签
     * @return 生成的命名空间
     * @throws ParamException
     */
    V1Namespace generateNamespace(String ccodVersion, String platformId, String platformTag) throws ParamException;

    /**
     * 生成平台初始job
     * @param platform 需要生成初始job的平台
     * @param isBase 该job是运行于base-platform.platformId命名空间还是platform.platformId命名空间
     * @return 平台初始job
     * @throws ParamException
     */
    List<V1Job> generatePlatformInitJob(PlatformPo platform, boolean isBase) throws ParamException;

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
     * @param domain 域相关信息
     * @param platform k8s平台相关信息
     * @return ccod域应用的k8s资源信息
     * @throws ParamException
     */
    K8sCCODDomainAppVo generateNewCCODDomainApp(AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform) throws ParamException, InterfaceCallException, IOException;


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
     * @param domain 域相关信息
     * @param platform k8s平台相关信息
     * @param isNewPlatform 该平台是否为新建平台
     * @return 添加域应用的k8s操作步骤
     * @throws ParamException
     * @throws ApiException
     */
    List<K8sOperationInfo> generateAddPlatformAppSteps(String jobId, AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform, boolean isNewPlatform) throws ParamException, ApiException, InterfaceCallException, IOException;

    /**
     * 生成修改ccod域应用的k8s操作步骤
     * @param jobId 任务id
     * @param appBase 应用相关基础信息
     * @param domain 域信息
     * @param platform k8s平台信息
     * @return 修改域应用的k8s操作步骤
     * @throws ParamException
     * @throws ApiException
     */
    List<K8sOperationInfo> generateUpdatePlatformAppSteps(String jobId, AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform) throws ParamException, ApiException, InterfaceCallException, IOException;

    /**
     * 生成修改ccod域应用的k8s调试步骤
     * @param jobId 任务id
     * @param appBase 应用相关基础信息
     * @param domain 域信息
     * @param platform k8s平台相关信息
     * @param timeout deployment状态变成Active的超时时长
     * @return 修改域应用的k8s调试步骤
     * @throws ParamException
     * @throws ApiException
     */
    List<K8sOperationInfo> generateDebugPlatformAppSteps(String jobId, AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform, int timeout) throws ParamException, ApiException, InterfaceCallException, IOException;

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
     * 生成平台创建步骤
     * @param jobId 创建平台的任务id
     * @param platform 需要被创建的平台
     * @param threePartApps 平台依赖的第三方应用
     * @return 平台创建所需执行的步骤
     * @throws ApiException
     * @throws ParamException
     * @throws IOException
     * @throws InterfaceCallException
     */
    List<K8sOperationInfo> generatePlatformCreateSteps(String jobId, PlatformPo platform, List<CCODThreePartAppPo> threePartApps) throws ApiException, ParamException, IOException, InterfaceCallException;

    /**
     * 生成只运行第三方应用的基础平台创建步骤
     * @param jobId 创建平台的任务id
     * @param platform 需要被创建的平台
     * @param threePartApps 平台依赖的第三方应用
     * @return 平台创建所需执行的步骤
     * @throws ApiException
     * @throws ParamException
     * @throws IOException
     * @throws InterfaceCallException
     */
    List<K8sOperationInfo> generateBasePlatformCreateSteps(
            String jobId, PlatformPo platform, List<CCODThreePartAppPo> threePartApps) throws ApiException, ParamException, IOException, InterfaceCallException;


    List<K8sOperationInfo> generateDomainDeploySteps(
            String jobId, PlatformPo platformPo, DomainUpdatePlanInfo plan, List<PlatformAppDeployDetailVo> domainApps,
            boolean isNewPlatform) throws ApiException, InterfaceCallException, IOException, ParamException;

    /**
     * 查询指定平台所有ccod模块在k8s上的运行状态
     * @param platform 指定的基于k8s的平台
     * @param isGetCfg 是否获取配置文件信息
     * @return 所有ccod模块在k8s上运行状态
     * @throws ApiException
     * @throws ParamException
     */
    List<PlatformAppDeployDetailVo> getPlatformAppDetailFromK8s(PlatformPo platform, boolean isGetCfg) throws ApiException, ParamException, InterfaceCallException, NexusException, IOException;

    /**
     * 查询指定ccod模块在k8s上的运行状态
     * @param platform 平台信息
     * @param domainId 域id
     * @param appName 应用名
     * @param alias 应用别名
     * @param isGetCfg 是否分析和下载应用配置
     * @return 指定ccod模块在k8s上的运行状态
     * @throws ApiException
     */
    PlatformAppDeployDetailVo getPlatformAppDetailFromK8s(PlatformPo platform, String domainId, String appName, String alias, boolean isGetCfg);

    /**
     * 查询指定条件的k8s对象模板
     * @param ccodVersion ccod版本
     * @param appType 应用类型
     * @param appName 应用名
     * @param version 应用版本
     * @return 指定条件的k8s对象模板
     */
    List<K8sObjectTemplatePo> queryK8sObjectTemplate(String ccodVersion, AppType appType, String appName, String version);

    /**
     * 查询满足指定标签的k8s对象模板
     * @param labels 用来查询的标签
     * @return 查询结果
     */
    List<K8sObjectTemplatePo> queryK8sObjectTemplate(Map<String, String> labels);

    /**
     * 向数据库添加一条新的k8s对象模板记录
     * @param template 对象模板
     * @throws ParamException
     */
    void addNewK8sObjectTemplate(K8sObjectTemplatePo template) throws ParamException;

    /**
     * 修改已有的k8s对象模板
     * @param template 需要修改的对象模板
     * @throws ParamException
     */
    void updateK8sObjectTemplate(K8sObjectTemplatePo template) throws ParamException;

    /**
     * 删除指定标签的k8s对象模板
     * @param labels 指定删除的k8s对象模板的标签
     * @throws ParamException
     */
    void deleteObjectTemplate(Map<String, String> labels) throws ParamException;

    /**
     * 从已有的平台模板克隆出新的平台模板
     * @param srcCcodVersion 被克隆的平台ccod版本号
     * @param srcPlatformTag 被克隆的平台标签
     * @param dstCcodVersion 新的平台ccod版本号
     * @param dstPlatformTag 新的平台标签
     * @return 克隆出的平台模板
     * @throws ParamException
     */
    List<K8sObjectTemplatePo> cloneExistPlatformTemplate(String srcCcodVersion, String srcPlatformTag, String dstCcodVersion, String dstPlatformTag) throws ParamException;

    /**
     * 克隆已经存在的ccod应用模板
     * @param srcCcodVersion 被克隆的平台ccod版本号
     * @param dstCcodVersion 新的平台ccod版本号
     * @return 克隆出的平台应用模板
     * @throws ParamException
     */
    List<K8sObjectTemplatePo> cloneExistAppTemplate(String srcCcodVersion, String dstCcodVersion) throws ParamException;

}
