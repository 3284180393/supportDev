package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.constant.ServicePortType;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;
import com.channelsoft.ccod.support.cmdb.k8s.vo.K8sCCODDomainAppVo;
import com.channelsoft.ccod.support.cmdb.po.PlatformPo;
import com.channelsoft.ccod.support.cmdb.vo.AppFileNexusInfo;
import com.channelsoft.ccod.support.cmdb.vo.AppModuleVo;
import com.channelsoft.ccod.support.cmdb.vo.AppUpdateOperationInfo;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;

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

    V1Namespace getNamespace(Map<String, String> selector, String platformId) throws ParamException;

    V1Secret getSecret(Map<String, String> selector, String platformId, String name) throws ParamException;

    V1PersistentVolume getPersistentVolume(Map<String, String> selector, String platformId) throws ParamException;

    V1PersistentVolumeClaim getPersistentVolumeClaim(Map<String, String> selector, String platformId) throws ParamException;

    K8sCCODDomainAppVo getCCODDomainApp(String alias, AppModuleVo module, String domainId, PlatformPo platform) throws ParamException, ApiException;
}
