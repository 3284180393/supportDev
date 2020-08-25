package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.constant.CCODPlatformStatus;

import java.util.*;

/**
 * @ClassName: PlatformPo
 * @Author: lanhb
 * @Description: 用来定义ccod平台的类
 * @Date: 2019/11/12 11:24
 * @Version: 1.0
 */
public class PlatformPo extends PlatformBase{

    private Date createTime; //创建时间

    private Date updateTime; //最后一次修改时间

    private CCODPlatformStatus status; //平台当前状态

    private String comment; //平台描述

    private Map<String, Object> params; //其它同平台有关的参数，例如oracle、mango连接方式等

    public PlatformPo(){}

    public PlatformPo(PlatformBase platformBase, CCODPlatformStatus status, String comment)
    {
        super(platformBase);
        this.status = status;
        this.comment = comment;
        this.params = getParams();
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

    public CCODPlatformStatus getStatus() {
        return status;
    }

    public void setStatus(CCODPlatformStatus status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

}
