package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.constant.ServicePortType;
import com.channelsoft.ccod.support.cmdb.vo.AppFileNexusInfo;
import com.channelsoft.ccod.support.cmdb.vo.AppModuleVo;

import java.util.Date;

/**
 * @ClassName: AppPo
 * @Author: lanhb
 * @Description: 用来定义app的类
 * @Date: 2019/11/12 17:26
 * @Version: 1.0
 */
public class AppPo extends AppBase{

    private int appId;    //应用id

    private boolean kernal; //该模块是否是核心模块，如果是核心模块则CREATE或是REPLACE对应的deployment时必须返回执行成功后才能执行后面操作

    private int timeout; //启动超时

    private Date createTime; //应用创建时间

    private Date updateTime; //应用最后一次修改时间

    private String createReason; //创建原因

    private String comment; //备注

    private String versionControl; //版本控制方式

    private String versionControlUrl; //版本控制的连接url

    private boolean hasImage; //是否有镜像

    private AppFileNexusInfo installPackage; //部署安装包

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

    public AppFileNexusInfo getInstallPackage() {
        return installPackage;
    }

    public void setInstallPackage(AppFileNexusInfo installPackage) {
        this.installPackage = installPackage;
    }
}
