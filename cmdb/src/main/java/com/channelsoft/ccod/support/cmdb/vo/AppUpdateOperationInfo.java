package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.AppUpdateOperation;
import com.channelsoft.ccod.support.cmdb.constant.UpdateStatus;
import com.channelsoft.ccod.support.cmdb.po.*;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
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

    private int platformAppId; //平台应用id,如果操作是ADD为0,否则是被操作的平台应用id

    @NotNull(message = "operation can not be null")
    private AppUpdateOperation operation; //应用升级类型,由AppUpdateType枚举定义

    private String domainId; //应用所在的域id

    private String domainName; //域名

    private String assembleTag; //应用所在assemble的标签

    private String originalAlias; //应用原始别名(用来被用户而不是被系统识别的别名)

    private String originalVersion; //操作前应用版本,如果是ADD操作，该属性为空

    private String hostIp; //应用所在的服务器ip

    private String appRunner; //该应用的执行用户

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

    public int getPlatformAppId() {
        return platformAppId;
    }

    public void setPlatformAppId(int platformAppId) {
        this.platformAppId = platformAppId;
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

    public AppUpdateOperationInfo(){}

    public AppUpdateOperationInfo(AppBase appBase)
    {
        super(appBase);
    }

    public PlatformAppPo getPlatformApp(int appId, String platformId, String domainId)
    {
        PlatformAppPo po = new PlatformAppPo(this);
        po.setPlatformId(platformId);
        po.setOriginalAlias(this.originalAlias);
        po.setDomainId(domainId);
        po.setDeployTime(new Date());
        po.setAppRunner(this.appRunner);
        po.setHostIp(this.hostIp);
        po.setAlias(this.alias);
        po.setAppId(appId);
        po.setPlatformAppId(0);
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

    public PlatformAppPo getPlatformApp(int platformAppId, int appId, String platformId, String domainId)
    {
        PlatformAppPo po = getPlatformApp(appId, platformId, domainId);
        po.setPlatformAppId(platformAppId);
        return po;
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
