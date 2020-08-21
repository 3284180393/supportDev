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
public class AppModuleVo extends AppBase{

    private int appId; //部署应用的id

    private Date createTime; //创建时间

    private String createReason; //创建原因

    private Date updateTime; //最后一次修改时间

    private VersionControl versionControl; //版本控制方式

    private String versionControlUrl; //版本控制的连接url

    private boolean kernal; //该模块是否是核心模块，如果是核心模块则CREATE或是REPLACE对应的deployment时必须返回执行成功后才能执行后面操作

    private int timeout; //启动超时

    private String comment; //备注

    private AppInstallPackagePo installPackage; //应用部署包

    private boolean hasImage; //是否有镜像

    public AppModuleVo()
    {

    }

    public AppModuleVo(AppPo app, AppInstallPackagePo installPackage, List<AppCfgFilePo> cfgs)
    {
        this.appId = app.getAppId();
        this.appType = app.getAppType();
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
        this.envLoadCmd = app.getEnvLoadCmd();
        this.initCmd = app.getInitCmd();
        this.startCmd = app.getStartCmd();
        this.logOutputCmd = app.getLogOutputCmd();
        this.kernal = app.isKernal();
        this.timeout = app.getTimeout();
        this.ports = app.getPorts();
        this.nodePorts = app.getNodePorts();
        this.checkAt = app.getCheckAt();
        this.resources = app.getResources();
        this.initialDelaySeconds = app.getInitialDelaySeconds();
        this.periodSeconds = app.getPeriodSeconds();
        this.comment = app.getComment();
        this.installPackage = installPackage;
        this.hasImage = app.isHasImage();
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
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

    public AppInstallPackagePo getInstallPackage() {
        return installPackage;
    }

    public void setInstallPackage(AppInstallPackagePo installPackage) {
        this.installPackage = installPackage;
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

    public String getAppNexusUploadUrl(String nexusHostUrl, String repository) {
        String url = String.format("%s/service/rest/v1/components?repository=%s", nexusHostUrl, repository);
        return url;
    }

    public boolean isHasImage() {
        return hasImage;
    }

    public void setHasImage(boolean hasImage) {
        this.hasImage = hasImage;
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
        po.setAppType(this.appType);
        po.setCcodVersion(this.ccodVersion);
        if(this.versionControl != null)
            po.setVersionControl(this.versionControl.name);
        else
            po.setVersionControl(null);
        po.setAppId(this.appId);
        po.setDeployPath(this.deployPath);
        po.setEnvLoadCmd(this.envLoadCmd);
        po.setInitCmd(this.initCmd);
        po.setStartCmd(this.startCmd);
        po.setLogOutputCmd(this.getLogOutputCmd());
        po.setTimeout(this.timeout);
        po.setResources(this.resources);
        po.setPorts(this.ports);
        po.setNodePorts(this.nodePorts);
        po.setCheckAt(this.checkAt);
        po.setResources(this.resources);
        po.setVersionControlUrl(this.versionControlUrl);
        po.setKernal(this.kernal);
        return po;
    }

    @Override
    public String toString()
    {
        return String.format("%s(%s)", this.appName, this.getVersion());
    }
}
