package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.constant.VersionControl;
import com.channelsoft.ccod.support.cmdb.po.AppCfgFilePo;
import com.channelsoft.ccod.support.cmdb.po.AppInstallPackagePo;
import com.channelsoft.ccod.support.cmdb.po.PlatformAppCfgFilePo;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: PlatformAppDeployDetailVo
 * @Author: lanhb
 * @Description: 用来定义平台应用部署情况
 * @Date: 2019/11/25 17:17
 * @Version: 1.0
 */
public class PlatformAppDeployDetailVo {

    private int platformAppId; //平台app部署id,数据库唯一生成

    private int appId; //应用id,外键app的appId

    private String platformId; //平台id

    private String platformName; //部署平台名

    private String domainId; //应用所在的域id,外键domain的domain_id

    private String domainName; //部署域名

    private String appName; //应用名

    private String appAlias; //应用别名

    private AppType appType; //应用类型

    private String hostname; //部署服务器名

    private String hostIp; //服务器ip

    private String appRunner; //运行用户名

    private String ccodVersion; //ccod版本

    private String basePath; //该应用的basePath

    private Date deployTime; //该应用的部署路径

    private String version; //应用版本

    private Date createTime; //应用创建时间

    private String versionControl; //版本控制方式

    private String versionControlUrl; //版本控制的连接url

    private AppInstallPackagePo installPackage; //应用部署包

    private List<AppCfgFilePo> srcCfgs; //原始配置文件

    private List<PlatformAppCfgFilePo> cfgs; //应用部署后的配置文件

    private int bkBizId; //该应用在蓝鲸biz id

    private String setId; //cmdb中标识该应用所属的set的id,setId在cmdb中不唯一,但platformName+setId在cmdb中唯一

    private int bkSetId; //该应用在paas的set id

    private String bkSetName; //该应用在paas的set名字

    private int bkModuleId; //该应用在蓝鲸paas的唯一id

    private int bkHostId; //该应用在蓝鲸paas的服务器id

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

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public Date getDeployTime() {
        return deployTime;
    }

    public void setDeployTime(Date deployTime) {
        this.deployTime = deployTime;
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

    public String getVersionControl() {
        return versionControl;
    }

    public void setVersionControl(String versionControl) {
        this.versionControl = versionControl;
    }

    public String getVersionControlUrl() {
        return versionControlUrl;
    }

    public void setVersionControlUrl(String versionControlUrl) {
        this.versionControlUrl = versionControlUrl;
    }

    public AppInstallPackagePo getInstallPackage() {
        return installPackage;
    }

    public void setInstallPackage(AppInstallPackagePo installPackage) {
        this.installPackage = installPackage;
    }

    public List<AppCfgFilePo> getSrcCfgs() {
        return srcCfgs;
    }

    public void setSrcCfgs(List<AppCfgFilePo> srcCfgs) {
        this.srcCfgs = srcCfgs;
    }

    public List<PlatformAppCfgFilePo> getCfgs() {
        return cfgs;
    }

    public void setCfgs(List<PlatformAppCfgFilePo> cfgs) {
        this.cfgs = cfgs;
    }

    public String getAppRunner() {
        return appRunner;
    }

    public void setAppRunner(String appRunner) {
        this.appRunner = appRunner;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public String getCcodVersion() {
        return ccodVersion;
    }

    public void setCcodVersion(String ccodVersion) {
        this.ccodVersion = ccodVersion;
    }

    public String getSetId() {
        return setId;
    }

    public void setSetId(String setId) {
        this.setId = setId;
    }

    public int getBkModuleId() {
        return bkModuleId;
    }

    public void setBkModuleId(int bkModuleId) {
        this.bkModuleId = bkModuleId;
    }

    public int getBkBizId() {
        return bkBizId;
    }

    public void setBkBizId(int bkBizId) {
        this.bkBizId = bkBizId;
    }

    public int getBkSetId() {
        return bkSetId;
    }

    public void setBkSetId(int bkSetId) {
        this.bkSetId = bkSetId;
    }

    public int getBkHostId() {
        return bkHostId;
    }

    public void setBkHostId(int bkHostId) {
        this.bkHostId = bkHostId;
    }

    public String getBkSetName() {
        return bkSetName;
    }

    public void setBkSetName(String bkSetName) {
        this.bkSetName = bkSetName;
    }
}
