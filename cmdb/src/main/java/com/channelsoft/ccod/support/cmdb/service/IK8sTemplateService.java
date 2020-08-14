package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.constant.K8sKind;
import com.channelsoft.ccod.support.cmdb.constant.ServicePortType;
import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;
import com.channelsoft.ccod.support.cmdb.k8s.vo.K8sCCODDomainAppVo;
import com.channelsoft.ccod.support.cmdb.po.PlatformPo;
import com.channelsoft.ccod.support.cmdb.vo.AppFileNexusInfo;
import com.channelsoft.ccod.support.cmdb.vo.AppModuleVo;
import com.channelsoft.ccod.support.cmdb.vo.AppUpdateOperationInfo;
import com.channelsoft.ccod.support.cmdb.vo.K8sOperationInfo;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;

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

    ExtensionsV1beta1Ingress getIngress(Map<String, String> selector, String alias, String platformId, String domainId, String hostUrl) throws ParamException;

    V1Service getService(Map<String, String> selector, String appName, String alias, AppType appType, ServicePortType portType, String portStr, String platformId, String domainId) throws ParamException;

    V1Service getService(Map<String, String> selector, String appName, String alias, String platformId) throws ParamException;

    V1Deployment getDeployment(AppUpdateOperationInfo optInfo, String hostUrl, String platformId, String domainId, List<AppFileNexusInfo> platformCfg, List<AppFileNexusInfo> domainCfg) throws ParamException;

    V1Deployment getDeployment(Map<String, String> selector, String appName, String alias, String version, String platformId) throws ParamException;

    V1Namespace generateNamespace(String ccodVersion, String platformId) throws ParamException;

    V1Secret getSecret(Map<String, String> selector, String platformId, String name) throws ParamException;

    V1PersistentVolume generatePersistentVolume(String ccodVersion, String platformId) throws ParamException;

    V1PersistentVolumeClaim generatePersistentVolumeClaim(String ccodVersion, String platformId) throws ParamException;

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
     * @param optInfo ccod域应用明细
     * @param domainId 域id
     * @param domainCfg 域公共配置
     * @param platformId 平台id
     * @param platformCfg 平台公共配置
     * @param hostUrl 平台访问域名
     * @param k8sApiUrl k8s的api的url
     * @param k8sAuthToken 访问k8s api的认证token
     * @return ccod域应用的k8s资源信息
     * @throws ParamException
     */
    K8sCCODDomainAppVo getNewCCODDomainApp(AppUpdateOperationInfo optInfo, String domainId, List<AppFileNexusInfo> domainCfg, String platformId, List<AppFileNexusInfo> platformCfg, String hostUrl, String k8sApiUrl, String k8sAuthToken) throws ParamException, ApiException, InterfaceCallException, IOException;

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
     * @param optInfo 需要添加的域应用相关信息
     * @param domainId 域id
     * @param domainCfg 域公共配置
     * @param platformId 平台id
     * @param platformCfg 平台公共配置
     * @param hostUrl 平台访问域名
     * @param k8sApiUrl k8s的api的url
     * @param k8sAuthToken 访问k8s api的认证token
     * @param isNewPlatform 该平台是否为新建平台
     * @return 添加域应用的k8s操作步骤
     * @throws ParamException
     * @throws ApiException
     */
    List<K8sOperationInfo> getAddPlatformAppSteps(
            String jobId, AppUpdateOperationInfo optInfo, String domainId, List<AppFileNexusInfo> domainCfg, String platformId,
            List<AppFileNexusInfo> platformCfg, String hostUrl, String k8sApiUrl, String k8sAuthToken, boolean isNewPlatform)
            throws ParamException, ApiException, InterfaceCallException, IOException;

    /**
     * 生成修改ccod域应用的k8s操作步骤
     * @param jobId 任务id
     * @param optInfo 需要添加的域应用相关信息
     * @param domainId 域id
     * @param domainCfg 域公共配置
     * @param platformId 平台id
     * @param platformCfg 平台公共配置
     * @param hostUrl 平台访问域名
     * @param k8sApiUrl k8s的api的url
     * @param k8sAuthToken 访问k8s api的认证token
     * @param isNewPlatform 该平台是否为新建平台
     * @return 修改域应用的k8s操作步骤
     * @throws ParamException
     * @throws ApiException
     */
    List<K8sOperationInfo> getUpdatePlatformAppSteps(
            String jobId, AppUpdateOperationInfo optInfo, String domainId, List<AppFileNexusInfo> domainCfg, String platformId,
            List<AppFileNexusInfo> platformCfg, String hostUrl, String k8sApiUrl, String k8sAuthToken)
            throws ParamException, ApiException, InterfaceCallException, IOException;

    /**
     * 生成选择器用于选择k8s上的ccod域应用相关资源
     * @param appName 应用名
     * @param alias 应用别名
     * @param version 应用版本
     * @param appType 应用类型
     * @param domainId 域id
     * @return 生成的选择器
     */
    Map<String, String> getCCODDomainAppSelector(String appName, String alias, String version, AppType appType, String domainId);

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

}
