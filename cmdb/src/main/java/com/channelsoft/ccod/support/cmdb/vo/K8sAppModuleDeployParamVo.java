package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.AppUpdateOperation;
import com.channelsoft.ccod.support.cmdb.constant.K8sOperation;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1Ingress;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Service;

import java.util.List;

/**
 * @ClassName: K8sAppModuleDeployParamVo
 * @Author: lanhb
 * @Description: 用来定义ccod app模块部署相关参数
 * @Date: 2020/7/13 15:45
 * @Version: 1.0
 */
public class K8sAppModuleDeployParamVo {

    private String platformId;

    private String jobId;

    private AppUpdateOperation operation;

    private AppModuleVo appModule;

    private String domainId;

    private String alias;

    private String assembleTag;

    private String basePath;

    private String deployPath;

    private List<AppFileNexusInfo> cfgs;

    private List<AppFileNexusInfo> domainConfig;

    private List<AppFileNexusInfo> platformConfig;

    private String startCmd;

    private String deploymentName;

    private List<V1Service> services;

    private List<ExtensionsV1beta1Ingress> ingresses;

    public K8sAppModuleDeployParamVo(String jobId, String platformId, String domainId, String assembleTag,
                                     AppUpdateOperationInfo optInfo, List<AppFileNexusInfo> platformConfig,
                                     List<AppFileNexusInfo> domainConfig, V1Deployment deployment,
                                     List<V1Service> services, List<ExtensionsV1beta1Ingress> ingresses) throws ParamException
    {
        this.jobId = jobId;
        this.platformId = platformId;
        this.domainId = domainId;
        this.assembleTag = assembleTag;
        this.operation = optInfo.getOperation();
        this.alias = optInfo.getAppAlias();
        this.basePath = optInfo.getBasePath();
        this.deployPath = optInfo.getDeployPath();
        this.cfgs = optInfo.getCfgs();
        this.domainConfig = domainConfig;
        this.platformConfig = domainConfig;
        this.startCmd = optInfo.getStartCmd();
        this.deploymentName = deployment.getMetadata().getName();
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public AppUpdateOperation getOperation() {
        return operation;
    }

    public void setOperation(AppUpdateOperation operation) {
        this.operation = operation;
    }

    public AppModuleVo getAppModule() {
        return appModule;
    }

    public void setAppModule(AppModuleVo appModule) {
        this.appModule = appModule;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getAssembleTag() {
        return assembleTag;
    }

    public void setAssembleTag(String assembleTag) {
        this.assembleTag = assembleTag;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getDeployPath() {
        return deployPath;
    }

    public void setDeployPath(String deployPath) {
        this.deployPath = deployPath;
    }

    public List<AppFileNexusInfo> getCfgs() {
        return cfgs;
    }

    public void setCfgs(List<AppFileNexusInfo> cfgs) {
        this.cfgs = cfgs;
    }

    public List<AppFileNexusInfo> getDomainConfig() {
        return domainConfig;
    }

    public void setDomainConfig(List<AppFileNexusInfo> domainConfig) {
        this.domainConfig = domainConfig;
    }

    public List<AppFileNexusInfo> getPlatformConfig() {
        return platformConfig;
    }

    public void setPlatformConfig(List<AppFileNexusInfo> platformConfig) {
        this.platformConfig = platformConfig;
    }

    public String getStartCmd() {
        return startCmd;
    }

    public void setStartCmd(String startCmd) {
        this.startCmd = startCmd;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public List<V1Service> getServices() {
        return services;
    }

    public void setServices(List<V1Service> services) {
        this.services = services;
    }

    public List<ExtensionsV1beta1Ingress> getIngresses() {
        return ingresses;
    }

    public void setIngresses(List<ExtensionsV1beta1Ingress> ingresses) {
        this.ingresses = ingresses;
    }
}
