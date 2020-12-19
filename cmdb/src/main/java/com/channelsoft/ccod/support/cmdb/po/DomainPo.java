package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.constant.DomainStatus;
import com.channelsoft.ccod.support.cmdb.constant.DomainType;
import com.channelsoft.ccod.support.cmdb.vo.AppFileNexusInfo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    private DomainType type; //域类型:是部署在实体机上，还是如k8s容器里

    private String platformId; //域所在的平台

    private Date createTime; //创建时间

    private Date updateTime; //修改时间

    private int status; //域状态

    private String comment; //备注

    private String bizSetName; //域归属业务集群名称

    private int occurs; //域的设计并发数

    private int maxOccurs; //域的最大并发数

    private String tag; //域的标签,例如:入呼叫、外呼、自动外拨

    private List<AppFileNexusInfo> cfgs; //域公共配置

    public DomainPo() {}

    public DomainPo(String domainId, String domainName, String platformId, DomainStatus status, String comment,
                    String bizSetName, int occurs, int maxOccurs, String tag)
    {
        Date now = new Date();
        this.domainId = domainId;
        this.domainName = domainName;
        this.platformId = platformId;
        this.createTime = now;
        this.updateTime = now;
        this.status = status.id;
        this.comment = comment;
        this.bizSetName = bizSetName;
        this.occurs = occurs;
        this.maxOccurs = maxOccurs;
        this.tag = tag;
        this.cfgs = new ArrayList<>();
    }

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

    public String getBizSetName() {
        return bizSetName;
    }

    public void setBizSetName(String bizSetName) {
        this.bizSetName = bizSetName;
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

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public DomainType getType() {
        return type;
    }

    public void setType(DomainType type) {
        this.type = type;
    }

    public List<AppFileNexusInfo> getCfgs() {
        return cfgs;
    }

    public void setCfgs(List<AppFileNexusInfo> cfgs) {
        this.cfgs = cfgs;
    }
}
