package com.channelsoft.ccod.support.cmdb.po;

import java.util.Date;

/**
 * @ClassName: DomainPo
 * @Author: lanhb
 * @Description: 用来定义域信息
 * @Date: 2019/11/12 14:46
 * @Version: 1.0
 */
public class DomainPo {

    private int domId; //id,数据库唯一主键

    private String domainId; //域id

    private String domainName; //域名

    private int platId; //该域归属的平台id,外键platform表主键

    private String platformId; //域所在的平台

    private int bkSetId; //该域在蓝鲸paas下的哪个set的id

    private Date createTime; //创建时间

    private Date updateTime; //修改时间

    private int status; //域状态

    private String comment; //备注

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getDomId() {
        return domId;
    }

    public void setDomId(int domId) {
        this.domId = domId;
    }

    public int getPlatId() {
        return platId;
    }

    public void setPlatId(int platId) {
        this.platId = platId;
    }

    public int getBkSetId() {
        return bkSetId;
    }

    public void setBkSetId(int bkSetId) {
        this.bkSetId = bkSetId;
    }
}
