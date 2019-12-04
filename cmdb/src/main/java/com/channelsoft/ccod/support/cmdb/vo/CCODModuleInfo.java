package com.channelsoft.ccod.support.cmdb.vo;

/**
 * @ClassName: CCODModuleInfo
 * @Author: lanhb
 * @Description: 用来定义域下的module信息
 * @Date: 2019/12/4 10:26
 * @Version: 1.0
 */
public class CCODModuleInfo {

    private String moduleName; //模块名

    private String moduleAlias; //模块别名

    private String version; //版本

    private String hostIp; //部署服务器ip

    private int platformAppId; //该模块信息在数据的id

    private int bkModuleId; //对应蓝鲸paas的module id

    private String versionControl; //版本控制方式

    private DownloadFileInfo installPackage; //安装包下载信息

    private DownloadFileInfo[] cfgs; //配置文件下载信息

    public CCODModuleInfo(LJModuleInfo moduleInfo)
    {
        this.bkModuleId = moduleInfo.getModuleId();
        this.moduleAlias = moduleInfo.getModuleName();
        this.moduleName = moduleInfo.getModuleName();
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


    public DownloadFileInfo getInstallPackage() {
        return installPackage;
    }

    public void setInstallPackage(DownloadFileInfo installPackage) {
        this.installPackage = installPackage;
    }

    public DownloadFileInfo[] getCfgs() {
        return cfgs;
    }

    public void setCfgs(DownloadFileInfo[] cfgs) {
        this.cfgs = cfgs;
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
}
