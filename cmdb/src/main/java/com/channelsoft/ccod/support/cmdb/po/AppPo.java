package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.constant.ServicePortType;
import com.channelsoft.ccod.support.cmdb.vo.AppModuleVo;

import java.util.Date;

/**
 * @ClassName: AppPo
 * @Author: lanhb
 * @Description: 用来定义app的类
 * @Date: 2019/11/12 17:26
 * @Version: 1.0
 */
public class AppPo {

    private int appId;    //应用id

    private String appName; //应用名

    private AppType appType; //应用类型

    private String version; //应用版本

    private String ccodVersion; //归属于哪个大版本的ccod

    private String basePath; //缺省的base path

    private String deployPath; //应用程序/包相对basePath的路径

    private String envLoadCmd; //配置创建命令

    private String initCmd; //初始化命令

    private String startCmd; //启动命令

    private String logOutputCmd; //日志输出命令

    private boolean kernal; //该模块是否是核心模块，如果是核心模块则CREATE或是REPLACE对应的deployment时必须返回执行成功后才能执行后面操作

    private int timeout; //启动超时

    private String ports; //该应用使用的端口

    private String nodePorts; //该应用对外开放的端口

    private String checkAt; //用来定义应用健康检查的端口以及协议

    private String resources; //启动该应用所需的资源

    private int initialDelaySeconds; //应用预计启动时间

    private int periodSeconds; //应用健康检查周期

    private Date createTime; //应用创建时间

    private Date updateTime; //应用最后一次修改时间

    private String createReason; //创建原因

    private String comment; //备注

    private String versionControl; //版本控制方式

    private String versionControlUrl; //版本控制的连接url

    private boolean hasImage; //是否有镜像

    public AppPo()
    {

    }

    public AppPo(AppModuleVo moduleVo, boolean hasImage)
    {
        this.appId = moduleVo.getAppId();
        this.appName = moduleVo.getAppName();
        this.appType = moduleVo.getAppType();
        this.version = moduleVo.getVersion();
        this.ccodVersion = moduleVo.getCcodVersion();
        this.basePath = moduleVo.getBasePath();
        this.deployPath = moduleVo.getDeployPath();
        this.envLoadCmd = moduleVo.getEnvLoadCmd();
        this.initCmd = moduleVo.getInitCmd();
        this.startCmd = moduleVo.getStartCmd();
        this.logOutputCmd = moduleVo.getLogOutputCmd();
        this.kernal = moduleVo.isKernal();
        this.timeout = moduleVo.getTimeout();
        this.ports = moduleVo.getPorts();
        this.nodePorts = moduleVo.getNodePorts();
        this.checkAt = moduleVo.getCheckAt();
        this.resources = moduleVo.getResources();
        this.initialDelaySeconds = moduleVo.getInitialDelaySeconds();
        this.periodSeconds = moduleVo.getPeriodSeconds();
        this.createTime = moduleVo.getCreateTime();
        this.updateTime = moduleVo.getUpdateTime();
        this.createReason = moduleVo.getCreateReason();
        this.comment = moduleVo.getComment();
        if(moduleVo.getVersionControl() != null)
            this.versionControl = moduleVo.getVersionControl().name;
        else
            this.versionControl = null;
        this.versionControlUrl = moduleVo.getVersionControlUrl();
        this.hasImage = hasImage;
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public AppType getAppType() {
        return appType;
    }

    public void setAppType(AppType appType) {
        this.appType = appType;
    }

    public String getCcodVersion() {
        return ccodVersion;
    }

    public void setCcodVersion(String ccodVersion) {
        this.ccodVersion = ccodVersion;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreateReason() {
        return createReason;
    }

    public void setCreateReason(String createReason) {
        this.createReason = createReason;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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

    public String getEnvLoadCmd() {
        return envLoadCmd;
    }

    public void setEnvLoadCmd(String envLoadCmd) {
        this.envLoadCmd = envLoadCmd;
    }

    public String getAppNexusDirectory() {
        String directory = String.format("%s/%s", this.appName, this.version);
        return directory;
    }

    public String getAppNexusUploadUrl(String nexusHostUrl, String repository) {
        String url = String.format("%s/service/rest/v1/components?repository=%s", nexusHostUrl, repository);
        return url;
    }

    public String getAppNexusGroup()
    {
        String group = String.format("/%s/%s", this.appName, this.version);
        return group;
    }

    public boolean isHasImage() {
        return hasImage;
    }

    public void setHasImage(boolean hasImage) {
        this.hasImage = hasImage;
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

    public boolean isKernal() {
        return kernal;
    }

    public void setKernal(boolean kernal) {
        this.kernal = kernal;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getInitCmd() {
        return initCmd;
    }

    public void setInitCmd(String initCmd) {
        this.initCmd = initCmd;
    }

    public String getResources() {
        return resources;
    }

    public void setResources(String resources) {
        this.resources = resources;
    }

    public String getLogOutputCmd() {
        return logOutputCmd;
    }

    public void setLogOutputCmd(String logOutputCmd) {
        this.logOutputCmd = logOutputCmd;
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

    public String getCheckAt() {
        return checkAt;
    }

    public void setCheckAt(String checkAt) {
        this.checkAt = checkAt;
    }
}
