package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.vo.AppUpdateOperationInfo;

import java.util.Date;

/**
 * @ClassName: AppDebugDetailPo
 * @Author: lanhb
 * @Description: 用来定义应用当前调试信息的pojo类
 * @Date: 2020/10/29 14:02
 * @Version: 1.0
 */
public class AppDebugDetailPo {

    private int id;  //id数据库主键

    private String platformId; //平台id

    private String domainId; //域id

    private String appName; //应用名

    private String alias; //应用别名

    private AppUpdateOperationInfo detail; //调试详情

    private Date createTime;  //调试创建时间

    private Date updateTime; //更新时间

    private boolean debugging; //是否正在调试

    private int tryCount; //调试次数

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public AppUpdateOperationInfo getDetail() {
        return detail;
    }

    public void setDetail(AppUpdateOperationInfo detail) {
        this.detail = detail;
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

    public boolean isDebugging() {
        return debugging;
    }

    public void setDebugging(boolean debugging) {
        this.debugging = debugging;
    }

    public int getTryCount() {
        return tryCount;
    }

    public void setTryCount(int tryCount) {
        this.tryCount = tryCount;
    }
}
