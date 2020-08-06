package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.constant.ServicePortType;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;
import com.channelsoft.ccod.support.cmdb.vo.AppFileNexusInfo;
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
@Service
public interface IK8sTemplateService {

    ExtensionsV1beta1Ingress selectIngress(Map<String, String> selector, String alias, String platformId, String domainId) throws ParamException;

    V1Service selectService(Map<String, String> selector, String appName, String alias, ServicePortType portType, String portStr, String platformId, String domainId) throws ParamException;

    V1Service selectService(Map<String, String> selector, String appName, String alias, String platformId) throws ParamException;

    V1Deployment selectDeployment(AppUpdateOperationInfo optInfo, String platformId, String domainId, List<AppFileNexusInfo> platformCfg, List<AppFileNexusInfo> domainCfg) throws ParamException;

    V1Deployment selectDeployment(Map<String, String> selector, String appName, String alias, String version, String platformId) throws ParamException;

    V1Namespace selectNamespace(Map<String, String> selector, String platformId) throws ParamException;

    V1Secret selectSecret(Map<String, String> selector, String platformId, String name) throws ParamException;

    V1PersistentVolume selectPersistentVolume(Map<String, String> selector) throws ParamException;

    V1PersistentVolumeClaim selectPersistentVolumeClaim(Map<String, String> selector, String platformId) throws ParamException;
}
