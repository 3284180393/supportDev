package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.po.PlatformAppPo;
import com.channelsoft.ccod.support.cmdb.po.ServerPo;
import com.channelsoft.ccod.support.cmdb.po.ServerUserPo;

import java.util.Date;
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

    private String moduleType; //模块类型

    private String version; //版本

    private String ccodVersion; //ccod版本

    private String nexusComponentId; //该应用模块在nexus存储的componentId

    private String basePath; //模块安装base path

    private DeployFileInfo installPackage; //安装包信息

    private DeployFileInfo[] cfgs; //安装包的配置文件列表

    private Map<String, String> attributes; //模块属性,以k,v形式展现

    private Date checkTime; //什么时间检查的

    private int sshPort; //ssh登录端口

    public Date getCheckTime() {
        return checkTime;
    }

    public void setCheckTime(Date checkTime) {
        this.checkTime = checkTime;
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
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

    public String getModuleType() {
        return moduleType;
    }

    public void setModuleType(String moduleType) {
        this.moduleType = moduleType;
    }

    public String getCcodVersion() {
        return ccodVersion;
    }

    public void setCcodVersion(String ccodVersion) {
        this.ccodVersion = ccodVersion;
    }

    public PlatformAppPo getPlatformApp()
    {
        PlatformAppPo po = new PlatformAppPo();
        po.setBasePath(this.basePath);
        po.setDeployTime(this.checkTime);
        po.setDomainId(this.platformId);
        po.setPlatformId(this.platformId);
        return po;
    }

    public ServerPo getServerInfo()
    {
        ServerPo po = new ServerPo();
        po.setComment("");
        po.setDomainId(this.domainId);
        po.setHostIp(this.hostIp);
        po.setHostname(this.hostName);
        po.setPlatformId(this.platformId);
        po.setServerType(1);
        po.setStatus(1);
        return po;
    }

    public ServerUserPo getServerUser()
    {
        ServerUserPo po = new ServerUserPo();
        po.setComment("");
        po.setLoginMethod(1);
        po.setPassword(this.password);
        po.setSshPort(this.sshPort);
        po.setUserName(this.loginUser);
        return po;
    }

    @Override
    public String toString()
    {
        String msg = String.format("platformId=%s,domainId=%s,hostIp=%s,appName=%s,appAlias=%s,version=%s,basePath=%s",
                this.platformId, this.domainId, this.hostIp, this.moduleName, this.getModuleAliasName(), this.version, this.basePath);
        return msg;
    }

}
