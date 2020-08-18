package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.AppUpdateOperation;
import com.channelsoft.ccod.support.cmdb.constant.UpdateStatus;
import com.channelsoft.ccod.support.cmdb.po.AppCfgFilePo;
import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;
import com.channelsoft.ccod.support.cmdb.po.PlatformAppCfgFilePo;
import com.channelsoft.ccod.support.cmdb.po.PlatformAppPo;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: AppUpdateOperationInfo
 * @Author: lanhb
 * @Description: 用来定义应用升级操作信息
 * @Date: 2019/12/11 17:27
 * @Version: 1.0
 */
@Validated
public class AppUpdateOperationInfo {

    private int platformAppId; //平台应用id,如果操作是ADD为0,否则是被操作的平台应用id

    @NotNull(message = "operation can not be null")
    private AppUpdateOperation operation; //应用升级类型,由AppUpdateType枚举定义

    private String domainId; //应用所在的域id

    private String domainName; //域名

    private String assembleTag; //应用所在assemble的标签

    @NotNull(message = "appName can not be null")
    private String appName; //应用名

    @NotNull(message = "appAlias can not be null")
    private String appAlias; //应用别名

    private String originalAlias; //应用原始别名(用来被用户而不是被系统识别的别名)

    private String originalVersion; //操作前应用版本,如果是ADD操作，该属性为空

    private String targetVersion; //操作后应用版本,如果是DELETE或是CFG_UPDATE、STOP、START操作则该参数为0

    private String hostIp; //应用所在的服务器ip

    private String basePath; //该应用所在的base path

    private String deployPath; //应用部署目录

    private String appRunner; //该应用的执行用户

    private String envLoadCmd; //环境加载命令

    private String initCmd; //初始化命令

    private String startCmd; //启动命令

    private String logOutputCmd; //日志输出命令

    private String ports; //该应用使用的端口

    private String nodePorts; //该应用对外开放的端口

    private String resources; //启动该应用所需的资源

    private int initialDelaySeconds; //应用预计启动时间

    private int periodSeconds; //应用健康检查周期

    private List<AppFileNexusInfo> cfgs; //如果升级成功,需要返回升级后的应用配置在nexus中的存储信息

    private int addDelay; //完成这条操作后延迟多少秒后执行下一条操作

    public AppUpdateOperation getOperation() {
        return operation;
    }

    public void setOperation(AppUpdateOperation operation) {
        this.operation = operation;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getOriginalVersion() {
        return originalVersion;
    }

    public void setOriginalVersion(String originalVersion) {
        this.originalVersion = originalVersion;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
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

    public List<AppFileNexusInfo> getCfgs() {
        return cfgs;
    }

    public void setCfgs(List<AppFileNexusInfo> cfgs) {
        this.cfgs = cfgs;
    }

    public String getAppAlias() {
        return appAlias;
    }

    public void setAppAlias(String appAlias) {
        this.appAlias = appAlias;
    }

    public int getPlatformAppId() {
        return platformAppId;
    }

    public void setPlatformAppId(int platformAppId) {
        this.platformAppId = platformAppId;
    }

    public int getAddDelay() {
        return addDelay;
    }

    public void setAddDelay(int addDelay) {
        this.addDelay = addDelay;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getOriginalAlias() {
        return originalAlias;
    }

    public void setOriginalAlias(String originalAlias) {
        this.originalAlias = originalAlias;
    }

    public String getAssembleTag() {
        return assembleTag;
    }

    public void setAssembleTag(String assembleTag) {
        this.assembleTag = assembleTag;
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

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
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

    public PlatformAppPo getPlatformApp(int appId, String platformId, String domainId)
    {
        PlatformAppPo po = new PlatformAppPo();
        po.setPlatformId(platformId);
        po.setOriginalAlias(this.originalAlias);
        po.setDomainId(domainId);
        po.setDeployTime(new Date());
        po.setBasePath(this.basePath);
        po.setAppRunner(this.appRunner);
        po.setHostIp(this.hostIp);
        po.setAppAlias(this.appAlias);
        po.setAppId(appId);
        po.setPlatformAppId(0);
        po.setEnvLoadCmd(this.envLoadCmd);
        po.setInitCmd(this.initCmd);
        po.setStartCmd(this.startCmd);
        po.setLogOutputCmd(this.logOutputCmd);
        po.setResources(this.resources);
        po.setInitialDelaySeconds(this.initialDelaySeconds);
        po.setPeriodSeconds(this.periodSeconds);
        po.setDeployPath(this.deployPath);
        return po;
    }

    public PlatformAppDeployDetailVo getPlatformAppDetail(String platformId, String nexusHostUrl)
    {
        PlatformAppDeployDetailVo vo = new PlatformAppDeployDetailVo();
        vo.setPlatformId(platformId);
        vo.setOriginalAlias(this.originalAlias);
        vo.setDomainId(domainId);
        vo.setDeployTime(new Date());
        vo.setBasePath(this.basePath);
        vo.setAppRunner(this.appRunner);
        vo.setHostIp(this.hostIp);
        vo.setAppAlias(this.appAlias);
        vo.setPlatformAppId(0);
        vo.setEnvLoadCmd(this.envLoadCmd);
        vo.setInitCmd(this.initCmd);
        vo.setStartCmd(this.startCmd);
        vo.setLogOutputCmd(this.logOutputCmd);
        vo.setResources(this.resources);
        vo.setInitialDelaySeconds(this.initialDelaySeconds);
        vo.setPeriodSeconds(this.periodSeconds);
        vo.setDeployPath(this.deployPath);
        vo.setAppName(this.appName);
        vo.setCfgs(this.cfgs.stream().map(cfg->new PlatformAppCfgFilePo(0, cfg, nexusHostUrl)).collect(Collectors.toList()));
        return vo;
    }

    public PlatformAppPo getPlatformApp(int platformAppId, int appId, String platformId, String domainId)
    {
        PlatformAppPo po = getPlatformApp(appId, platformId, domainId);
        po.setPlatformAppId(platformAppId);
        return po;
    }

    @Override
    public String toString()
    {
        String tag = String.format("%s %s(%s)", operation.name, appAlias, appName);
        String desc;
        switch (operation)
        {
            case ADD:
                desc = String.format("%s to %s", targetVersion, hostIp);
                break;
            case DELETE:
                desc = String.format("from %s", hostIp);
                break;
            default:
                desc = String.format("at %s", hostIp);

        }
        return String.format("%s %s", tag, desc);
    }
}
