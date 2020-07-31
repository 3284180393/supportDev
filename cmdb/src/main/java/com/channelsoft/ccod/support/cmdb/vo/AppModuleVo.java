package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.constant.ServicePortType;
import com.channelsoft.ccod.support.cmdb.constant.VersionControl;
import com.channelsoft.ccod.support.cmdb.po.*;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: AppModuleVo
 * @Author: lanhb
 * @Description: 用来定义应用模块信息的类
 * @Date: 2019/11/14 14:34
 * @Version: 1.0
 */
public class AppModuleVo {

    private int appId; //部署应用的id

    private AppType appType; //应用类型

    private String appName; //应用名

    private String version; //应用版本

    private String ccodVersion; //对应的ccod大版本

    private Date createTime; //创建时间

    private String createReason; //创建原因

    private Date updateTime; //最后一次修改时间

    private VersionControl versionControl; //版本控制方式

    private String versionControlUrl; //版本控制的连接url

    private String basePath; //应用的base path

    private String deployPath; //应用程序/包相对basePath的路径

    private String startCmd; //启动命令

    private String ports; //应用使用的端口

    private String nodePorts; //应用对外开放的端口

    private boolean kernal; //该模块是否是核心模块，如果是核心模块则CREATE或是REPLACE对应的deployment时必须返回执行成功后才能执行后面操作

    private int timeout; //启动超时

    private String comment; //备注

    private AppInstallPackagePo installPackage; //应用部署包

    private List<AppCfgFilePo> cfgs; //应用配置文件

    private boolean hasImage; //是否有镜像

    public AppModuleVo()
    {

    }

    public AppModuleVo(AppPo app, AppInstallPackagePo installPackage, List<AppCfgFilePo> cfgs)
    {
        this.appId = app.getAppId();
        this.appType = AppType.getEnum(app.getAppType());
        this.appName = app.getAppName();
        this.version = app.getVersion();
        this.ccodVersion = app.getCcodVersion();
        this.createTime = app.getCreateTime();
        this.createReason = app.getCreateReason();
        this.updateTime = app.getUpdateTime();
        this.versionControl = VersionControl.getEnum(app.getVersionControl());
        this.versionControlUrl = app.getVersionControlUrl();
        this.basePath = app.getBasePath();
        this.deployPath = app.getDeployPath();
        this.startCmd = app.getStartCmd();
        this.ports = app.getPorts();
        this.nodePorts = app.getNodePorts();
        this.comment = app.getComment();
        this.installPackage = installPackage;
        this.cfgs = cfgs;
        this.hasImage = app.isHasImage();
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public VersionControl getVersionControl() {
        return versionControl;
    }

    public void setVersionControl(VersionControl versionControl) {
        this.versionControl = versionControl;
    }

    public String getVersionControlUrl() {
        return versionControlUrl;
    }

    public void setVersionControlUrl(String versionControlUrl) {
        this.versionControlUrl = versionControlUrl;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public AppInstallPackagePo getInstallPackage() {
        return installPackage;
    }

    public void setInstallPackage(AppInstallPackagePo installPackage) {
        this.installPackage = installPackage;
    }

    public List<AppCfgFilePo> getCfgs() {
        return cfgs;
    }

    public void setCfgs(List<AppCfgFilePo> cfgs) {
        this.cfgs = cfgs;
    }

    public String getCcodVersion() {
        return ccodVersion;
    }

    public void setCcodVersion(String ccodVersion) {
        this.ccodVersion = ccodVersion;
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

    public AppPo getApp()
    {
        AppPo po = new AppPo();
        po.setHasImage(this.hasImage);
        po.setBasePath(this.basePath);
        po.setVersion(this.version);
        po.setAppName(this.appName);
        po.setUpdateTime(this.updateTime);
        po.setCreateTime(this.createTime);
        po.setCreateReason(this.createReason);
        po.setComment(this.comment);
        po.setAppType(this.appType.name);
        po.setCcodVersion(this.ccodVersion);
        if(this.versionControl != null)
            po.setVersionControl(this.versionControl.name);
        else
            po.setVersionControl(null);
        po.setAppId(this.appId);
        po.setDeployPath(this.deployPath);
        po.setStartCmd(this.startCmd);
        po.setPorts(this.ports);
        po.setNodePorts(this.nodePorts);
        po.setVersionControlUrl(this.versionControlUrl);
        po.setKernal(this.kernal);
        po.setTimeout(this.timeout);
        return po;
    }

    @Override
    public String toString()
    {
        return String.format("%s(%s)", this.appName, this.getVersion());
    }
}
