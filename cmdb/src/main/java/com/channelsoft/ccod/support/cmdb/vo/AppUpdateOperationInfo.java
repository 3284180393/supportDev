package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.AppUpdateOperation;
import com.channelsoft.ccod.support.cmdb.constant.UpdateStatus;
import com.channelsoft.ccod.support.cmdb.po.AppCfgFilePo;
import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;
import com.channelsoft.ccod.support.cmdb.po.PlatformAppPo;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: AppUpdateOperationInfo
 * @Author: lanhb
 * @Description: 用来定义应用升级操作信息
 * @Date: 2019/12/11 17:27
 * @Version: 1.0
 */
public class AppUpdateOperationInfo {

    private int platformAppId; //平台应用id,如果操作是ADD为0,否则是被操作的平台应用id

    private AppUpdateOperation operation; //应用升级类型,由AppUpdateType枚举定义

    private String domainId; //应用所在的域id

    private String appName; //应用名

    private String appAlias; //应用别名

    private String originalAlias; //应用原始别名(用来被用户而不是被系统识别的别名)

    private String originalVersion; //操作前应用版本,如果是ADD操作，该属性为空

    private String targetVersion; //操作后应用版本,如果是DELETE或是CFG_UPDATE、STOP、START操作则该参数为0

    private String hostIp; //应用所在的服务器ip

    private String basePath; //该应用所在的base path

    private String appRunner; //该应用的执行用户

    private List<AppFileNexusInfo> cfgs; //如果升级成功,需要返回升级后的应用配置在nexus中的存储信息

    private int addDelay; //完成这条操作后延迟多少秒后执行下一条操作

    public AppUpdateOperation getOperation() {
        return operation;
    }

    public void setOperation(AppUpdateOperation operation) {
        this.operation = operation;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getOriginalVersion() {
        return originalVersion;
    }

    public void setOriginalVersion(String originalVersion) {
        this.originalVersion = originalVersion;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
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

    public String getAppAlias() {
        return appAlias;
    }

    public void setAppAlias(String appAlias) {
        this.appAlias = appAlias;
    }

    public int getPlatformAppId() {
        return platformAppId;
    }

    public void setPlatformAppId(int platformAppId) {
        this.platformAppId = platformAppId;
    }

    public int getAddDelay() {
        return addDelay;
    }

    public void setAddDelay(int addDelay) {
        this.addDelay = addDelay;
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

    public PlatformAppPo getPlatformApp(int appId, String platformId, String domainId)
    {
        PlatformAppPo po = new PlatformAppPo();
        po.setPlatformId(platformId);
        po.setOriginalAlias(this.originalAlias);
        po.setDomainId(domainId);
        po.setDeployTime(new Date());
        po.setBasePath(this.basePath);
        po.setAppRunner(this.appRunner);
        po.setHostIp(this.hostIp);
        po.setAppAlias(this.appAlias);
        po.setAppId(appId);
        po.setPlatformAppId(0);
        return po;
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
        String tag = String.format("%s %s(%s)", operation.name, appAlias, appName);
        String desc;
        switch (operation)
        {
            case ADD:
                desc = String.format("%s to %s", targetVersion, hostIp);
                break;
            case DELETE:
                desc = String.format("from %s", hostIp);
                break;
            case VERSION_UPDATE:
                desc = String.format("from %s to %s at %s", originalVersion, targetVersion, hostIp);
                break;
            case CFG_UPDATE:
                desc = String.format("cfg at %s", hostIp);
                break;
            default:
                desc = String.format("at %s", hostIp);

        }
        return String.format("%s %s", tag, desc);
    }
}
