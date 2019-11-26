package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.po.DomainPo;
import com.channelsoft.ccod.support.cmdb.po.ServerPo;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: PlatformResourceVo
 * @Author: lanhb
 * @Description: 用来定义平台资源信息
 * @Date: 2019/11/26 22:39
 * @Version: 1.0
 */
public class PlatformResourceVo {
    private String platformId; //平台id

    private String platformName; //平台名

    private int status; //平台状态

    private Date createTime; //平台创建时间

    private Date updateTime; //平台最后一次修改时间

    private String ccodVersion; //平台使用的ccod版本

    private String comment; //备注

    private List<DomainPo> domainList; //平台下面所属域列表

    private List<ServerPo> serverList; //平台下所属服务器列表

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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
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

    public String getCcodVersion() {
        return ccodVersion;
    }

    public void setCcodVersion(String ccodVersion) {
        this.ccodVersion = ccodVersion;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<DomainPo> getDomainList() {
        return domainList;
    }

    public void setDomainList(List<DomainPo> domainList) {
        this.domainList = domainList;
    }

    public List<ServerPo> getServerList() {
        return serverList;
    }

    public void setServerList(List<ServerPo> serverList) {
        this.serverList = serverList;
    }
}
