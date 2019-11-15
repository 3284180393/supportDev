package com.channelsoft.ccod.support.cmdb.vo;

import java.util.Map;

/**
 * @ClassName: PlatformAppModuleVo
 * @Author: lanhb
 * @Description: 用来定义平台模块收集信息
 * @Date: 2019/11/14 20:33
 * @Version: 1.0
 */
public class PlatformAppModuleVo {

    private String serialNo;  //序列号,每次检查唯一

    private String platformId; //平台id

    private String platformName; //平台名

    private String domainId; //域id

    private String domainName; //域名

    private String hostIp; //主机ip

    private String hostName; //主机名

    private String loginUser; //主机用户

    private String password; //用户登录密码

    private String moduleName; //模块名

    private String moduleAliasName; //模块别名

    private String version; //版本

    private String basePath; //模块安装base path

    private String deployPath; //安装目录

    private String installPackage; //安装包

    private String installPackageMd5; //安装包md5特征值

    private String installPackageLocalSavePath; //安装包传输过来后本地存放路径

    private AppModuleCfgVo[] cfgs; //安装包的配置文件列表

    private Map<String, String> attributes; //模块属性,以k,v形式展现

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

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
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

    public String getInstallPackage() {
        return installPackage;
    }

    public void setInstallPackage(String installPackage) {
        this.installPackage = installPackage;
    }

    public String getInstallPackageMd5() {
        return installPackageMd5;
    }

    public void setInstallPackageMd5(String installPackageMd5) {
        this.installPackageMd5 = installPackageMd5;
    }

    public AppModuleCfgVo[] getCfgs() {
        return cfgs;
    }

    public void setCfgs(AppModuleCfgVo[] cfgs) {
        this.cfgs = cfgs;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getLoginUser() {
        return loginUser;
    }

    public void setLoginUser(String loginUser) {
        this.loginUser = loginUser;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public String getInstallPackageLocalSavePath() {
        return installPackageLocalSavePath;
    }

    public void setInstallPackageLocalSavePath(String installPackageLocalSavePath) {
        this.installPackageLocalSavePath = installPackageLocalSavePath;
    }

    public String getModuleAliasName() {
        return moduleAliasName;
    }

    public void setModuleAliasName(String moduleAliasName) {
        this.moduleAliasName = moduleAliasName;
    }
}
