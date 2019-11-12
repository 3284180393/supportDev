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

    private int cfgId;    //应用配置id

    private int appTemplateId; //应用对应的模板id

    private String appName; //应用名

    private String appAlias; //应用别名

    private String version; //应用版本

    private String defaultDeployPath; //缺省安装路径

    private String defaultBasePath; //缺省的base path

    private Date createTime; //应用创建时间

    private Date updateTime; //应用最后一次修改时间

    private String createReason; //创建原因

    private String comment; //备注

    public int getCfgId() {
        return cfgId;
    }

    public void setCfgId(int cfgId) {
        this.cfgId = cfgId;
    }

    public int getAppTemplateId() {
        return appTemplateId;
    }

    public void setAppTemplateId(int appTemplateId) {
        this.appTemplateId = appTemplateId;
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

    public String getDefaultDeployPath() {
        return defaultDeployPath;
    }

    public void setDefaultDeployPath(String defaultDeployPath) {
        this.defaultDeployPath = defaultDeployPath;
    }

    public String getDefaultBasePath() {
        return defaultBasePath;
    }

    public void setDefaultBasePath(String defaultBasePath) {
        this.defaultBasePath = defaultBasePath;
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
}
