package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.DomainUpdateType;
import com.channelsoft.ccod.support.cmdb.constant.UpdateStatus;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: DomainUpdatePlanInfo
 * @Author: lanhb
 * @Description: 用来定义域升级方案信息的类
 * @Date: 2019/12/11 17:44
 * @Version: 1.0
 */
public class DomainUpdatePlanInfo {

    private int domId; //对应的域id

    private String domainName; //对应的域名

    private String domainId; //对应的域标识

    private List<AppUpdateOperationInfo> appUpdateOperationList; //应用升级操作列表

    private DomainUpdateType updateType; //该域升级方案类型,由DomainUpdateType枚举定义

    private UpdateStatus status; //该升级方案当前状态,由DomainUpdateStatus枚举定义

    private Date createTime; //该方案创建时间

    private Date updateTime; //方案最后一次修改时间

    private Date executeTime; //计划执行时间

    private String comment; //备注

    public List<AppUpdateOperationInfo> getAppUpdateOperationList() {
        return appUpdateOperationList;
    }

    public void setAppUpdateOperationList(List<AppUpdateOperationInfo> appUpdateOperationList) {
        this.appUpdateOperationList = appUpdateOperationList;
    }

    public DomainUpdateType getUpdateType() {
        return updateType;
    }

    public void setUpdateType(DomainUpdateType updateType) {
        this.updateType = updateType;
    }

    public UpdateStatus getStatus() {
        return status;
    }

    public void setStatus(UpdateStatus status) {
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

    public Date getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(Date executeTime) {
        this.executeTime = executeTime;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getDomId() {
        return domId;
    }

    public void setDomId(int domId) {
        this.domId = domId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }
}
