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

    private int platformAppId; //该模块信息在数据的id

    private int appId; //该模块对应的appId,该id由数据库唯一生成

    private String moduleName; //模块名

    private String moduleAlias; //模块别名

    private String originalAlias; //模块的原始别名

    private String version; //版本

    private String hostIp; //部署服务器ip

    private String port; //模块的相关port

    private String status; //模块的status

    private String basePath; //应用base path

    private String appRunner; //应用运行用户

    private AppInstallPackagePo installPackage; //应用安装包

    private List<PlatformAppCfgFilePo> cfgs; //应用部署后的配置文件

    public CCODModuleInfo(LJModuleInfo moduleInfo)
    {
        this.moduleAlias = moduleInfo.getBkModuleName();
        this.moduleName = moduleInfo.getBkModuleName();
    }

    public CCODModuleInfo(PlatformAppDeployDetailVo deployApp)
    {
        this.appId = deployApp.getAppId();
        this.moduleName = deployApp.getAppName();
        this.moduleAlias = deployApp.getAppAlias();
        this.originalAlias = deployApp.getOriginalAlias();
        this.version = deployApp.getVersion();
        this.hostIp = deployApp.getHostIp();
        this.platformAppId = deployApp.getPlatformAppId();
        this.basePath = deployApp.getBasePath();
        this.appRunner = deployApp.getAppRunner();
        this.installPackage = deployApp.getInstallPackage();
        this.cfgs = deployApp.getCfgs();


    }

    public CCODModuleInfo()
    {

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

    public void setCfgs(List<PlatformAppCfgFilePo> cfgs) {
        this.cfgs = cfgs;
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public String getOriginalAlias() {
        return originalAlias;
    }

    public void setOriginalAlias(String originalAlias) {
        this.originalAlias = originalAlias;
    }

    @Override
    public CCODModuleInfo clone()
    {
        CCODModuleInfo module = new CCODModuleInfo();
        module.appId = this.appId;
        module.cfgs = this.cfgs;
        module.installPackage = this.installPackage;
        module.appRunner = this.appRunner;
        module.basePath = this.basePath;
        module.platformAppId = this.platformAppId;
        module.moduleAlias = this.moduleAlias;
        module.moduleName = this.moduleName;
        module.hostIp = this.hostIp;
        module.originalAlias = this.originalAlias;
        return module;
    }
}
