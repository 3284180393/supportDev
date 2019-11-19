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

    private String nexusComponentId; //该应用模块在nexus存储的componentId

    private String basePath; //模块安装base path

    private DeployFileInfo installPackage; //安装包信息

    private DeployFileInfo[] cfgs; //安装包的配置文件列表

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

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getModuleAliasName() {
        return moduleAliasName;
    }

    public void setModuleAliasName(String moduleAliasName) {
        this.moduleAliasName = moduleAliasName;
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

    public DeployFileInfo getInstallPackage() {
        return installPackage;
    }

    public void setInstallPackage(DeployFileInfo installPackage) {
        this.installPackage = installPackage;
    }

    public DeployFileInfo[] getCfgs() {
        return cfgs;
    }

    public void setCfgs(DeployFileInfo[] cfgs) {
        this.cfgs = cfgs;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getNexusComponentId() {
        return nexusComponentId;
    }

    public void setNexusComponentId(String nexusComponentId) {
        this.nexusComponentId = nexusComponentId;
    }
}
