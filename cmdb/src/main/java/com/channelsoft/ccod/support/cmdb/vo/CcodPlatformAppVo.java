package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.po.*;

import java.util.Date;

/**
 * @ClassName: CcodPlatformAppVo
 * @Author: lanhb
 * @Description: 用来定义ccod平台应用相关信息
 * @Date: 2019/11/12 17:37
 * @Version: 1.0
 */
public class CcodPlatformAppVo {
    private int platformAppId; //对应PlatformAppPo中的platformAppId,唯一主键

    private int appId; //部署应用的id

    private String appType; //应用类型

    private String appName; //应用名

    private String appAlias; //应用别名

    private String version; //应用版本

    private String basePath; //应用的base path

    private String deployPath; //应用的部署路径

    private String platformId; //应用部署平台id

    private String platformName; //应用部署平台名

    private String domainId; //应用部署域id

    private String domainName; //应用部署域名

    private ServerPo deployServer; //部署服务器信息

    private ServerUserPo appRunner; //app的运行用户

    private AppPackagePo packageInfo; //app的部署安装包

    private AppCfgFilePo[] cfgFiles; //应用的配置文件

    public int getPlatformAppId() {
        return platformAppId;
    }

    public void setPlatformAppId(int platformAppId) {
        this.platformAppId = platformAppId;
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
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

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public ServerPo getDeployServer() {
        return deployServer;
    }

    public void setDeployServer(ServerPo deployServer) {
        this.deployServer = deployServer;
    }

    public ServerUserPo getAppRunner() {
        return appRunner;
    }

    public void setAppRunner(ServerUserPo appRunner) {
        this.appRunner = appRunner;
    }

    public AppPackagePo getPackageInfo() {
        return packageInfo;
    }

    public void setPackageInfo(AppPackagePo packageInfo) {
        this.packageInfo = packageInfo;
    }

    public AppCfgFilePo[] getCfgFiles() {
        return cfgFiles;
    }

    public void setCfgFiles(AppCfgFilePo[] cfgFiles) {
        this.cfgFiles = cfgFiles;
    }
}
