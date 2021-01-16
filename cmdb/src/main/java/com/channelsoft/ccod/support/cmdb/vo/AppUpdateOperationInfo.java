package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.AppUpdateOperation;
import com.channelsoft.ccod.support.cmdb.constant.UpdateStatus;
import com.channelsoft.ccod.support.cmdb.po.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: AppUpdateOperationInfo
 * @Author: lanhb
 * @Description: 用来定义应用升级操作信息
 * @Date: 2019/12/11 17:27
 * @Version: 1.0
 */
@Validated
public class AppUpdateOperationInfo extends AppBase {

    @NotNull(message = "operation can not be null")
    private AppUpdateOperation operation; //应用升级类型,由AppUpdateType枚举定义

    private String platformId; //平台id

    private String domainId; //应用所在的域id

    private String domainName; //域名

    private String assembleTag; //应用所在assemble的标签

    private String originalAlias; //应用原始别名(用来被用户而不是被系统识别的别名)

    private String originalVersion; //操作前应用版本,如果是ADD操作，该属性为空

    private String hostIp; //应用所在的服务器ip

    private boolean fixedIp; //在容器化部署中应用的ip是否是固定的；true该应用将被布置在指定ip的node上，否则将有k8s自动配置,非容器化部署该值固定为true

    private String appRunner; //该应用的执行用户

    private List<AppFileNexusInfo> domainCfg;  //域公共配置，该字段主要用于调试应用

    private Integer timeout; //指定应用启动超时时长

    private Map<String, Object> runtime;  //应用的运行环境

    private String tag; //应用标签

    public AppUpdateOperationInfo(AppBase appBase)
    {
        super(appBase);
    }

    public AppUpdateOperationInfo(){}

    public AppUpdateOperation getOperation() {
        return operation;
    }

    public void setOperation(AppUpdateOperation operation) {
        this.operation = operation;
    }

    public String getOriginalVersion() {
        return originalVersion;
    }

    public void setOriginalVersion(String originalVersion) {
        this.originalVersion = originalVersion;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public String getAppRunner() {
        return appRunner;
    }

    public void setAppRunner(String appRunner) {
        this.appRunner = appRunner;
    }

    public List<AppFileNexusInfo> getCfgs() {
        return cfgs;
    }

    public void setCfgs(List<AppFileNexusInfo> cfgs) {
        this.cfgs = cfgs;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getOriginalAlias() {
        return originalAlias;
    }

    public void setOriginalAlias(String originalAlias) {
        this.originalAlias = originalAlias;
    }

    public String getAssembleTag() {
        return assembleTag;
    }

    public void setAssembleTag(String assembleTag) {
        this.assembleTag = assembleTag;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public List<AppFileNexusInfo> getDomainCfg() {
        return domainCfg;
    }

    public void setDomainCfg(List<AppFileNexusInfo> domainCfg) {
        this.domainCfg = domainCfg;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public Map<String, Object> getRuntime() {
        return runtime;
    }

    public void setRuntime(Map<String, Object> runtime) {
        this.runtime = runtime;
    }

    public String getDeployName(){
        return String.format("%s-%s", alias, domainId);
    }

    public boolean isFixedIp() {
        return fixedIp;
    }

    public void setFixedIp(boolean fixedIp) {
        this.fixedIp = fixedIp;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public PlatformAppPo getPlatformApp(int appId, List<AppFileNexusInfo> cfgs, String platformId, String domainId, int assembleId)
    {
        PlatformAppPo po = new PlatformAppPo(this, appId, cfgs, platformId, domainId, assembleId, originalAlias, hostIp, fixedIp,appRunner, tag);
        return po;
    }

    public PlatformAppDeployDetailVo getPlatformAppDetail(String platformId, String nexusHostUrl)
    {
        PlatformAppDeployDetailVo vo = new PlatformAppDeployDetailVo(this);
        vo.setPlatformId(platformId);
        vo.setOriginalAlias(this.originalAlias);
        vo.setDomainId(domainId);
        vo.setDeployTime(new Date());
        vo.setAppRunner(this.appRunner);
        vo.setHostIp(this.hostIp);
        vo.setAlias(this.alias);
        vo.setPlatformAppId(0);
        vo.setCfgs(cfgs);
        return vo;
    }

    public Map<String, Object> getHostAppInfo()
    {
        Map<String, Object> info = new HashMap<>();
        info.put("appName", appName);
        info.put("alias", alias);
        info.put("appType", appType.name);
        info.put("version", version);
        info.put("hostIp", hostIp);
        if(cfgs != null && cfgs.size() > 0){
            List<Map<String, String>> cfgParams = cfgs.stream().map(c->c.getHostFileInfo()).collect(Collectors.toList());
            info.put("cfgs", cfgParams);
        }
        Map<String, String> pkg = new HashMap<>();
        pkg.put("fileName", installPackage.getFileName());
        pkg.put("deployPath", installPackage.getDeployPath());
        pkg.put("md5", installPackage.getMd5());
        info.put("installPackage", pkg);
        if(StringUtils.isNotBlank(checkAt)){
            info.put("checkAt", checkAt);
        }
        if(timeout != null && timeout > 0){
            info.put("timeout", timeout);
        }
        if(runtime != null){
            info.put("runtime", runtime);
        }
        if(StringUtils.isNotBlank(envLoadCmd)){
            info.put("envLoadCmd", envLoadCmd);
        }
        info.put("startCmd", startCmd);
        if(StringUtils.isNotBlank(logOutputCmd)){
            info.put("logOutputCmd", logOutputCmd);
        }
        return info;
    }

    @Override
    public String toString()
    {
        String tag = String.format("%s %s(%s)", operation.name, alias, appName);
        String desc;
        switch (operation)
        {
            case ADD:
                desc = String.format("%s to %s", version, hostIp);
                break;
            case DELETE:
                desc = String.format("from %s", hostIp);
                break;
            default:
                desc = String.format("at %s", hostIp);

        }
        return String.format("%s %s", tag, desc);
    }

    public Map<String, String> getK8sMacroData(DomainPo domain, PlatformPo platform){
        Map<String, String> data = new HashMap<>();
        data.put(K8sObjectTemplatePo.APP_NAME, appName);
        data.put(K8sObjectTemplatePo.ALIAS, alias);
        data.put(K8sObjectTemplatePo.DOMAIN_ID, domain.getDomainId());
        data.put(K8sObjectTemplatePo.APP_LOW_NAME, appName.toLowerCase());
        data.put(K8sObjectTemplatePo.APP_VERSION, version.replace(":", "-"));
        data.put(K8sObjectTemplatePo.HOST_URL, platform.getHostUrl());
        data.put(K8sObjectTemplatePo.K8S_HOST_IP, platform.getK8sHostIp() == null ? (String)platform.getParams().get(PlatformBase.k8sHostIpKey) : platform.getK8sHostIp());
        data.put(K8sObjectTemplatePo.NFS_SERVER_IP, platform.getNfsServerIp() == null ? (String)platform.getParams().get(PlatformBase.nfsServerIpKey) : platform.getNfsServerIp());
        data.put(K8sObjectTemplatePo.PLATFORM_ID, platform.getPlatformId());
        data.put(K8sObjectTemplatePo.APP_TYPE, appType.name);
        return data;
    }
}
