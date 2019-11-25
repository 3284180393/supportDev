package com.channelsoft.ccod.support.cmdb.po;

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

    private String appAlias; //应用别名

    private String appType; //应用类型

    private String version; //应用版本

    private String ccodVersion; //归属于哪个大版本的ccod

    private String basePath; //缺省的base path

    private Date createTime; //应用创建时间

    private Date updateTime; //应用最后一次修改时间

    private String createReason; //创建原因

    private String comment; //备注

    private String versionControl; //版本控制方式

    private String versionControlUrl; //版本控制的连接url

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
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
}
