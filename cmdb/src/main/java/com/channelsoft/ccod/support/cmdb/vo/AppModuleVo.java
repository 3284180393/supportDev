package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.constant.VersionControl;
import com.channelsoft.ccod.support.cmdb.po.NexusComponentPo;

import java.util.Date;

/**
 * @ClassName: AppModuleVo
 * @Author: lanhb
 * @Description: 用来定义应用模块信息的类
 * @Date: 2019/11/14 14:34
 * @Version: 1.0
 */
public class AppModuleVo {
    private int appId; //部署应用的id

    private AppType appType; //应用类型

    private String appName; //应用名

    private String appAlias; //应用别名

    private String version; //应用版本

    private Date createTime; //创建时间

    private VersionControl versionControl; //版本控制方式

    private String versionControlUrl; //版本控制的连接url

    private String basePath; //应用的base path

    private String deployPath; //应用的部署路径

    private String cfgPath; //配置文件路径

    private NexusComponentPo installPackage; //发布包在nexus的存储信息

    private NexusComponentPo[] cfgs; //配置文件在nexus的存储信息

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public AppType getAppType() {
        return appType;
    }

    public void setAppType(AppType appType) {
        this.appType = appType;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppAlias() {
        return appAlias;
    }

    public void setAppAlias(String appAlias) {
        this.appAlias = appAlias;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public VersionControl getVersionControl() {
        return versionControl;
    }

    public void setVersionControl(VersionControl versionControl) {
        this.versionControl = versionControl;
    }

    public String getVersionControlUrl() {
        return versionControlUrl;
    }

    public void setVersionControlUrl(String versionControlUrl) {
        this.versionControlUrl = versionControlUrl;
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

    public String getCfgPath() {
        return cfgPath;
    }

    public void setCfgPath(String cfgPath) {
        this.cfgPath = cfgPath;
    }

    public NexusComponentPo getInstallPackage() {
        return installPackage;
    }

    public void setInstallPackage(NexusComponentPo installPackage) {
        this.installPackage = installPackage;
    }

    public NexusComponentPo[] getCfgs() {
        return cfgs;
    }

    public void setCfgs(NexusComponentPo[] cfgs) {
        this.cfgs = cfgs;
    }
}
