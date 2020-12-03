package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.AppUpdateOperation;
import com.channelsoft.ccod.support.cmdb.constant.UpdateStatus;
import com.channelsoft.ccod.support.cmdb.po.*;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

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

    private String appRunner; //该应用的执行用户

    private List<AppFileNexusInfo> domainCfg;  //域公共配置，该字段主要用于调试应用

    private Integer timeout; //指定应用启动超时时长

    private Map<String, Object> runtime;  //应用的运行环境

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

    public PlatformAppPo getPlatformApp(int appId, List<AppFileNexusInfo> cfgs, String platformId, String domainId, int assembleId)
    {
        PlatformAppPo po = new PlatformAppPo(this, appId, cfgs, platformId, domainId, assembleId, originalAlias, hostIp, appRunner);
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
}
