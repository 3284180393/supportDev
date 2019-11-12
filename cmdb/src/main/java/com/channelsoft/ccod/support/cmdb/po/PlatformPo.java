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
    private String platformId; //平台id,数据库唯一主键

    private String platformName; //平台名

    private Date createTime; //创建时间

    private Date updateTime; //最后一次修改时间

    private int status; //平台当前状态

    private String comment; //平台描述

    private String cfgReportBrokeUrl; //获取该平台配置的mq broke url

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

    public String getCfgReportBrokeUrl() {
        return cfgReportBrokeUrl;
    }

    public void setCfgReportBrokeUrl(String cfgReportBrokeUrl) {
        this.cfgReportBrokeUrl = cfgReportBrokeUrl;
    }
}
