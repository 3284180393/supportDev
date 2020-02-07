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

    private String domainId; //域id

    private String domainName; //域名

    private String platformId; //域所在的平台

    private Date createTime; //创建时间

    private Date updateTime; //修改时间

    private int status; //域状态

    private String comment; //备注

    private int occurs; //域的设计并发数

    private int maxOccurs; //域的最大并发数

    private String tags; //域的标签,例如:入呼叫、外呼、自动外拨

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

    public int getOccurs() {
        return occurs;
    }

    public void setOccurs(int occurs) {
        this.occurs = occurs;
    }

    public int getMaxOccurs() {
        return maxOccurs;
    }

    public void setMaxOccurs(int maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}
