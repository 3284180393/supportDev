package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.config.BizSetDefine;
import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.constant.AppUpdateOperation;
import com.channelsoft.ccod.support.cmdb.constant.VersionControl;
import com.channelsoft.ccod.support.cmdb.po.AppCfgFilePo;
import com.channelsoft.ccod.support.cmdb.po.AppInstallPackagePo;
import com.channelsoft.ccod.support.cmdb.po.PlatformAppCfgFilePo;
import com.channelsoft.ccod.support.cmdb.po.PlatformAppPo;
import io.kubernetes.client.openapi.models.V1ConfigMap;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private int assembleId; //应用所在assemble的id

    private String assembleTag; //应用所在assemble的tag

    private String appName; //应用名

    private String appAlias; //应用别名

    private String originalAlias; //原始别名

    private AppType appType; //应用类型

    private String hostname; //部署服务器名

    private String hostIp; //服务器ip

    private String port; //应用的相关port

    private int replicas; //运行副本数目

    private int availableReplicas; //可用副本数目

    private String status; //应用的相关status

    private String appRunner; //运行用户名

    private String ccodVersion; //ccod版本

    private String basePath; //该应用的basePath

    private String deployPath; //应用程序/安装包部署路径

    private String envLoadCmd; //配置创建命令

    private String initCmd; //初始化命令

    private String startCmd; //启动命令

    private String logOutputCmd; //日志输出命令

    private String ports; //该应用使用的端口

    private String nodePorts; //该应用对外开放的端口

    private String checkAt; //用来定义应用健康检查的端口以及协议

    private String resources; //启动该应用所需的资源

    private int initialDelaySeconds; //应用预计启动时间

    private int periodSeconds; //应用健康检查周期

    private Date deployTime; //该应用的部署路径

    private String version; //应用版本

    private Date createTime; //应用创建时间

    private String versionControl; //版本控制方式

    private String versionControlUrl; //版本控制的连接url

    private AppInstallPackagePo installPackage; //应用部署包

    private List<AppCfgFilePo> srcCfgs; //原始配置文件

    private List<PlatformAppCfgFilePo> cfgs; //应用部署后的配置文件

    private int bkBizId; //该应用在蓝鲸biz id

    private int bkSetId; //该应用在paas的set id

    private String bkSetName; //该应用在paas的set名字

    private int bkModuleId; //该应用在蓝鲸paas的唯一id

    private int bkHostId; //该应用在蓝鲸paas的服务器id

    public PlatformAppDeployDetailVo()
    {}

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

    public String getOriginalAlias() {
        return originalAlias;
    }

    public void setOriginalAlias(String originalAlias) {
        this.originalAlias = originalAlias;
    }

    public int getAssembleId() {
        return assembleId;
    }

    public void setAssembleId(int assembleId) {
        this.assembleId = assembleId;
    }

    public String getAssembleTag() {
        return assembleTag;
    }

    public void setAssembleTag(String assembleTag) {
        this.assembleTag = assembleTag;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }

    public int getAvailableReplicas() {
        return availableReplicas;
    }

    public void setAvailableReplicas(int availableReplicas) {
        this.availableReplicas = availableReplicas;
    }

    public String getDeployPath() {
        return deployPath;
    }

    public void setDeployPath(String deployPath) {
        this.deployPath = deployPath;
    }

    public String getStartCmd() {
        return startCmd;
    }

    public void setStartCmd(String startCmd) {
        this.startCmd = startCmd;
    }

    public String getInitCmd() {
        return initCmd;
    }

    public void setInitCmd(String initCmd) {
        this.initCmd = initCmd;
    }

    public String getLogOutputCmd() {
        return logOutputCmd;
    }

    public void setLogOutputCmd(String logOutputCmd) {
        this.logOutputCmd = logOutputCmd;
    }

    public String getPorts() {
        return ports;
    }

    public void setPorts(String ports) {
        this.ports = ports;
    }

    public String getNodePorts() {
        return nodePorts;
    }

    public void setNodePorts(String nodePorts) {
        this.nodePorts = nodePorts;
    }

    public String getResources() {
        return resources;
    }

    public void setResources(String resources) {
        this.resources = resources;
    }

    public int getInitialDelaySeconds() {
        return initialDelaySeconds;
    }

    public void setInitialDelaySeconds(int initialDelaySeconds) {
        this.initialDelaySeconds = initialDelaySeconds;
    }

    public int getPeriodSeconds() {
        return periodSeconds;
    }

    public void setPeriodSeconds(int periodSeconds) {
        this.periodSeconds = periodSeconds;
    }

    public String getEnvLoadCmd() {
        return envLoadCmd;
    }

    public void setEnvLoadCmd(String envLoadCmd) {
        this.envLoadCmd = envLoadCmd;
    }

    public String getCheckAt() {
        return checkAt;
    }

    public void setCheckAt(String checkAt) {
        this.checkAt = checkAt;
    }

    public PlatformAppPo getPlatformApp()
    {
        PlatformAppPo po = new PlatformAppPo();
        po.setOriginalAlias(this.originalAlias);
        po.setDomainId(this.domainId);
        po.setDeployTime(this.deployTime);
        po.setBasePath(this.basePath);
        po.setDeployPath(this.deployPath);
        po.setEnvLoadCmd(this.envLoadCmd);
        po.setInitCmd(this.initCmd);
        po.setStartCmd(this.startCmd);
        po.setLogOutputCmd(this.logOutputCmd);
        po.setPort(this.port);
        po.setNodePorts(this.nodePorts);
        po.setCheckAt(this.checkAt);
        po.setResources(this.resources);
        po.setInitialDelaySeconds(this.initialDelaySeconds);
        po.setPeriodSeconds(this.periodSeconds);
        po.setPlatformId(this.platformId);
        po.setAppId(this.appId);
        po.setAppRunner(this.appRunner);
        po.setHostIp(this.hostIp);
        po.setAppAlias(this.appAlias);
        po.setPlatformAppId(this.platformAppId);
        po.setAssembleId(this.assembleId);
        po.setReplicas(this.replicas);
        po.setAvailableReplicas(this.availableReplicas);
        return po;
    }

    public AppUpdateOperationInfo getOperationInfo(AppUpdateOperation operation)
    {
        AppUpdateOperationInfo optInfo = new AppUpdateOperationInfo();
        optInfo.setDomainId(this.domainId);
        optInfo.setEnvLoadCmd(this.envLoadCmd);
        optInfo.setAssembleTag(this.getAssembleTag());
        optInfo.setPorts(this.ports);
        optInfo.setNodePorts(this.nodePorts);
        optInfo.setStartCmd(this.getStartCmd());
        optInfo.setInitCmd(this.initCmd);
        optInfo.setLogOutputCmd(this.logOutputCmd);
        optInfo.setResources(this.resources);
        optInfo.setInitialDelaySeconds(this.initialDelaySeconds);
        optInfo.setHostIp(this.hostIp);
        optInfo.setEnvLoadCmd(this.envLoadCmd);
        optInfo.setPeriodSeconds(this.periodSeconds);
        optInfo.setTargetVersion(this.version);
        optInfo.setBasePath(this.basePath);
        optInfo.setDeployPath(this.deployPath);
        optInfo.setCfgs(this.getCfgs().stream().map(cfg->cfg.getAppFileNexusInfo()).collect(Collectors.toList()));
        optInfo.setAppRunner(this.appRunner);
        optInfo.setDomainName(this.domainName);
        optInfo.setOperation(operation);
        optInfo.setOriginalVersion(this.originalAlias);
        optInfo.setOriginalAlias(this.originalAlias);
        optInfo.setAppAlias(this.appAlias);
        optInfo.setAppName(this.appName);
        optInfo.setPlatformAppId(this.platformAppId);
        optInfo.setCheckAt(this.checkAt);
        return optInfo;
    }

    public AppUpdateOperationInfo getOperationInfo(AppUpdateOperation operation, String originalVersion)
    {
        AppUpdateOperationInfo optInfo = getOperationInfo(operation);
        optInfo.setOriginalVersion(originalVersion);
        return optInfo;
    }

    public String getCfgNexusDirectory(String tag)
    {
        return String.format(String.format("%s/%s/%s/%s/%s/%s", platformId, tag, domainId, appName, appAlias, version));
    }

    public String getCfgNexusDirectory(Date date)
    {
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        return getCfgNexusDirectory(sf.format(date));
    }
}
