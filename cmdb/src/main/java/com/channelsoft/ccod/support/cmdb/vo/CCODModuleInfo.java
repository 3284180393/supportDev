package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.po.AppCfgFilePo;
import com.channelsoft.ccod.support.cmdb.po.AppInstallPackagePo;
import com.channelsoft.ccod.support.cmdb.po.PlatformAppCfgFilePo;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: CCODModuleInfo
 * @Author: lanhb
 * @Description: 用来定义域下的module信息
 * @Date: 2019/12/4 10:26
 * @Version: 1.0
 */
public class CCODModuleInfo {

    private int appId; //该模块对应的appId,该id由数据库唯一生成

    private String moduleName; //模块名

    private String moduleAlias; //模块别名

    private String version; //版本

    private String hostIp; //部署服务器ip

    private String basePath; //应用base path

    private String appRunner; //应用运行用户

    private int platformAppId; //该模块信息在数据的id

    private int bkModuleId; //对应蓝鲸paas的module id

    private int bkHostId; //对应的蓝鲸host id

    private String versionControl; //版本控制方式

    private AppInstallPackagePo installPackage; //应用安装包

    private List<AppCfgFilePo> srcCfgs; //原始配置文件信息

    private List<PlatformAppCfgFilePo> cfgs; //应用部署后的配置文件

    public CCODModuleInfo(LJModuleInfo moduleInfo)
    {
        this.bkModuleId = moduleInfo.getBkModuleId();
        this.moduleAlias = moduleInfo.getBkModuleName();
        this.moduleName = moduleInfo.getBkModuleName();
    }

    public CCODModuleInfo(PlatformAppDeployDetailVo deployApp)
    {
        this.appId = deployApp.getAppId();
        this.versionControl = deployApp.getVersionControl();
        this.moduleName = deployApp.getAppName();
        this.moduleAlias = deployApp.getAppAlias();
        this.bkModuleId = deployApp.getBkModuleId();
        this.version = deployApp.getVersion();
        this.hostIp = deployApp.getHostIp();
        this.platformAppId = deployApp.getPlatformAppId();
        this.basePath = deployApp.getBasePath();
        this.appRunner = deployApp.getAppRunner();
        this.installPackage = deployApp.getInstallPackage();
        this.srcCfgs = deployApp.getSrcCfgs();
        this.cfgs = deployApp.getCfgs();
        this.bkHostId = deployApp.getBkHostId();
    }

    public CCODModuleInfo()
    {

    }

    public int getBkHostId() {
        return bkHostId;
    }

    public void setBkHostId(int bkHostId) {
        this.bkHostId = bkHostId;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getModuleAlias() {
        return moduleAlias;
    }

    public void setModuleAlias(String moduleAlias) {
        this.moduleAlias = moduleAlias;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getBkModuleId() {
        return bkModuleId;
    }

    public void setBkModuleId(int bkModuleId) {
        this.bkModuleId = bkModuleId;
    }

    public String getVersionControl() {
        return versionControl;
    }

    public void setVersionControl(String versionControl) {
        this.versionControl = versionControl;
    }

    public AppInstallPackagePo getInstallPackage() {
        return installPackage;
    }

    public List<PlatformAppCfgFilePo> getCfgs() {
        return cfgs;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public int getPlatformAppId() {
        return platformAppId;
    }

    public void setPlatformAppId(int platformAppId) {
        this.platformAppId = platformAppId;
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

    public void setInstallPackage(AppInstallPackagePo installPackage) {
        this.installPackage = installPackage;
    }

    public List<AppCfgFilePo> getSrcCfgs() {
        return srcCfgs;
    }

    public void setSrcCfgs(List<AppCfgFilePo> srcCfgs) {
        this.srcCfgs = srcCfgs;
    }

    public void setCfgs(List<PlatformAppCfgFilePo> cfgs) {
        this.cfgs = cfgs;
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    @Override
    public CCODModuleInfo clone()
    {
        CCODModuleInfo module = new CCODModuleInfo();
        module.appId = this.appId;
        module.cfgs = this.cfgs;
        module.srcCfgs = this.srcCfgs;
        module.installPackage = this.installPackage;
        module.appRunner = this.appRunner;
        module.basePath = this.basePath;
        module.platformAppId = this.platformAppId;
        module.versionControl = this.versionControl;
        module.moduleAlias = this.moduleAlias;
        module.moduleName = this.moduleName;
        module.hostIp = this.hostIp;
        module.bkModuleId = bkModuleId;
        module.bkHostId = this.bkHostId;
        return module;
    }
}
