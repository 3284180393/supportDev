package com.channelsoft.ccod.support.cmdb.po;

import java.util.Date;

/**
 * @ClassName: PlatformPo
 * @Author: lanhb
 * @Description: 用来定义ccod平台的类
 * @Date: 2019/11/12 11:24
 * @Version: 1.0
 */
public class PlatformPo {

    private String platformId; //平台id

    private String platformName; //平台名

    private int bkBizId; //平台在蓝鲸paas平台上的唯一id,平台名应当同paas上的bkBizName相同

    private int bkCloudId; //平台主机在蓝鲸paas的cloud id

    private Date createTime; //创建时间

    private Date updateTime; //最后一次修改时间

    private int status; //平台当前状态

    private String ccodVersion; //该平台采用的ccod版本

    private String comment; //平台描述

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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCcodVersion() {
        return ccodVersion;
    }

    public void setCcodVersion(String ccodVersion) {
        this.ccodVersion = ccodVersion;
    }

    public int getBkBizId() {
        return bkBizId;
    }

    public void setBkBizId(int bkBizId) {
        this.bkBizId = bkBizId;
    }

    public int getBkCloudId() {
        return bkCloudId;
    }

    public void setBkCloudId(int bkCloudId) {
        this.bkCloudId = bkCloudId;
    }
}
