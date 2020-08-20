package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.config.BizSetDefine;
import com.channelsoft.ccod.support.cmdb.constant.*;
import com.channelsoft.ccod.support.cmdb.po.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    private String moduleAliasName; //客户端提交的模块别名

    private String alias; //模块在域的标准别名，alias和moduleAliasName可能会不一样

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

    private String versionControl; //版本控制方式

    private String versionControlUrl; //版本控制的连接url

    private String comment; //备注

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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public PlatformPo getPlatform()
    {
        PlatformPo po = new PlatformPo(this.platformId, this.platformName, 0, 0,
                CCODPlatformStatus.RUNNING, this.ccodVersion, "create by auto data collected", PlatformType.PHYSICAL_MACHINE,
                PlatformFunction.ONLINE, PlatformCreateMethod.ONLINE_MANAGER_COLLECT, String.format("%s.ccod.com", platformId));
//        po.setCcodVersion(this.ccodVersion);
//        po.setComment("");
//        po.setCreateTime(this.checkTime);
//        po.setPlatformId(this.platformId);
//        po.setPlatformName(this.platformName);
//        po.setStatus(1);
//        po.setUpdateTime(new Date());
        return po;
    }

    public DomainPo getDomain()
    {
        DomainPo po = new DomainPo(this.domainId, this.domainName, this.platformId, DomainStatus.RUNNING,
                "created from collected domain data", "未处理类型", 400, 600, "online");
//        po.setComment("");
//        po.setCreateTime(this.checkTime);
//        po.setDomainId(this.domainId);
//        po.setDomainName(this.domainName);
//        po.setPlatformId(platformId);
//        po.setStatus(1);
//        po.setUpdateTime(new Date());
        return po;
    }

    public PlatformAppPo getPlatformApp()
    {
        PlatformAppPo po = new PlatformAppPo();
        po.setBasePath(this.basePath);
        po.setDeployTime(this.checkTime);
        po.setPlatformId(this.platformId);
        po.setDomainId(this.domainId);
        po.setDeployTime(new Date());
        po.setAppRunner(this.loginUser);
        po.setHostIp(this.hostIp);
        po.setAlias(this.alias);
        po.setOriginalAlias(this.moduleAliasName);
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

    public AppPo getAppInfo()
    {
        AppPo po = new AppPo();
        po.setVersion(this.version);
        po.setUpdateTime(new Date());
        po.setCreateTime(this.checkTime);
        po.setCreateReason("client report");
        po.setComment("");
        po.setCcodVersion(this.ccodVersion);
        po.setBasePath(this.basePath);
        if(this.moduleType == "CCOD_KERNEL_MODULE")
            po.setAppType(AppType.BINARY_FILE);
        else if(this.moduleType == "CCOD_WEBAPPS_MODULE")
            po.setAppType(AppType.RESIN_WEB_APP);
        else
            po.setAppType(AppType.getEnum(this.moduleType));
        po.setAppName(this.moduleName);
        po.setVersionControl(this.versionControl);
        po.setVersionControlUrl(this.versionControlUrl);
        return po;
    }

    public AppInstallPackagePo getAppInstallPackage()
    {
        AppInstallPackagePo packagePo = new AppInstallPackagePo();
        packagePo.setNexusRepository(installPackage.getNexusRepository());
        packagePo.setNexusAssetId(installPackage.getNexusAssetId());
        packagePo.setMd5(installPackage.getFileMd5());
        packagePo.setFileName(installPackage.getFileName());
        packagePo.setDeployPath(installPackage.getDeployPath());
        packagePo.setCreateTime(new Date());
        packagePo.setExt(installPackage.getExt());
        packagePo.setNexusDirectory(installPackage.getNexusDirectory());
        return packagePo;
    }

    public AppCfgFilePo[] getAppCfgs()
    {
        List<AppCfgFilePo> list = new ArrayList<>();
        for(DeployFileInfo cfg : this.cfgs)
        {
            AppCfgFilePo cfgFilePo = new AppCfgFilePo();
            cfgFilePo.setNexusAssetId(cfg.getNexusAssetId());
            cfgFilePo.setMd5(cfg.getFileMd5());
            cfgFilePo.setFileName(cfg.getFileName());
            cfgFilePo.setDeployPath(cfg.getDeployPath());
            cfgFilePo.setCreateTime(new Date());
            cfgFilePo.setExt(cfg.getExt());
            cfgFilePo.setNexusDirectory(cfg.getNexusDirectory());
            list.add(cfgFilePo);
        }
        return list.toArray(new AppCfgFilePo[0]);
    }


    public PlatformAppCfgFilePo[] getPlatformAppCfgFiles()
    {
        List<PlatformAppCfgFilePo> list = new ArrayList<>();
        for(DeployFileInfo cfg : this.cfgs)
        {
            PlatformAppCfgFilePo cfgFilePo = new PlatformAppCfgFilePo();
            cfgFilePo.setNexusAssetId(cfg.getNexusAssetId());
            cfgFilePo.setMd5(cfg.getFileMd5());
            cfgFilePo.setFileName(cfg.getFileName());
            cfgFilePo.setDeployPath(cfg.getDeployPath());
            cfgFilePo.setCreateTime(new Date());
            cfgFilePo.setExt(cfg.getExt());
            cfgFilePo.setNexusDirectory(cfg.getNexusDirectory());
            cfgFilePo.setNexusRepository(cfg.getNexusRepository());
            list.add(cfgFilePo);
        }
        return list.toArray(new PlatformAppCfgFilePo[0]);
    }

    public boolean isOk(String platformId, String platformName, Map<String, List<BizSetDefine>>appSetRelation)
    {
        boolean ok =  false;
        if(!platformId.equals(this.platformId))
            this.comment = String.format("platformId error, want %s and report %s", platformId, this.platformId);
        else if(!platformName.equals(this.platformName))
            this.comment = String.format("platformName error, want %s and report %s", platformName, this.platformName);
        else if(!appSetRelation.containsKey(this.moduleName))
            this.comment = String.format("app %s not been supported", this.moduleName);
        else if(!this.installPackage.isTransferSucc())
            this.comment = StringUtils.isNotBlank(this.installPackage.getTransferFailReason()) ? this.installPackage.getTransferFailReason() : String.format("not receive %s", this.installPackage.getFileName());
        else
        {
            boolean cfgOk = true;
            for(DeployFileInfo cfg : cfgs)
            {
                if(!cfg.isTransferSucc())
                {
                    cfgOk = false;
                    this.comment = StringUtils.isNotBlank(cfg.getTransferFailReason()) ? cfg.getTransferFailReason() : String.format("not receive %s", cfg.getFileName());
                    break;
                }
            }
            if(cfgOk)
                ok = true;
        }
        return ok;
    }

    public UnconfirmedAppModulePo getUnconfirmedModule()
    {
        UnconfirmedAppModulePo po = new UnconfirmedAppModulePo();
        po.setAlias(this.moduleAliasName);
        po.setAppName(this.moduleName);
        po.setDomainName(this.domainName);
        po.setHostIp(this.hostIp);
        po.setPlatformId(this.platformId);
        po.setSubmitTime(new Date());
        po.setReason(this.comment);
        return po;
    }

    @Override
    public String toString()
    {
        String msg = String.format("%s(%s=%s) at %s in %s",
                this.moduleAliasName, this.moduleName, this.version, this.hostIp, this.domainId);
        return msg;
    }

}
