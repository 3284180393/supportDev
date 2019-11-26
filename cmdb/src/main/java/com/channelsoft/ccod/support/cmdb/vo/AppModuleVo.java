package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.constant.VersionControl;
import com.channelsoft.ccod.support.cmdb.po.AppCfgFilePo;
import com.channelsoft.ccod.support.cmdb.po.AppInstallPackagePo;
import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;
import com.channelsoft.ccod.support.cmdb.po.NexusComponentPo;

import java.util.Date;
import java.util.List;

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

    private String ccodVersion; //对应的ccod大版本

    private Date createTime; //创建时间

    private String createReason; //创建原因

    private Date updateTime; //最后一次修改时间

    private VersionControl versionControl; //版本控制方式

    private String versionControlUrl; //版本控制的连接url

    private String basePath; //应用的base path

    private String comment; //备注

    private AppInstallPackagePo installPackage; //应用部署包

    private List<AppCfgFilePo> cfgs; //应用配置文件

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

    public AppInstallPackagePo getInstallPackage() {
        return installPackage;
    }

    public void setInstallPackage(AppInstallPackagePo installPackage) {
        this.installPackage = installPackage;
    }

    public List<AppCfgFilePo> getCfgs() {
        return cfgs;
    }

    public void setCfgs(List<AppCfgFilePo> cfgs) {
        this.cfgs = cfgs;
    }

    public String getCcodVersion() {
        return ccodVersion;
    }

    public void setCcodVersion(String ccodVersion) {
        this.ccodVersion = ccodVersion;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreateReason() {
        return createReason;
    }

    public void setCreateReason(String createReason) {
        this.createReason = createReason;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
