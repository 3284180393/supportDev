package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.PlatformUpdateTaskType;
import com.channelsoft.ccod.support.cmdb.constant.UpdateStatus;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: PlatformUpdateSchemaInfo
 * @Author: lanhb
 * @Description: 用来定义平台升级计划的类
 * @Date: 2019/12/11 17:47
 * @Version: 1.0
 */
public class PlatformUpdateSchemaInfo {

    private List<DomainUpdatePlanInfo> domainUpdatePlanList; //域升级方案列表

    private int bkBizId; //该平台对应蓝鲸的bizId

    private String platformName; //该平台的平台名,需要同蓝鲸的对应的bizName一致

    private PlatformUpdateTaskType taskType; //升级计划的任务类型,由PlatformUpdateTaskType枚举定义

    private UpdateStatus status; //任务当前状态,由PlatformUpdateTaskStatus枚举定义

    private Date createTime; //计划创建时间

    private Date updateTime; //计划最后一次修改时间

    private Date executeTime; //如果该时间为非空，在该时间自动执行

    private Date deadline; //计划完成最后期限

    private String title; //升级任务标题

    private String comment; //备注

    public List<DomainUpdatePlanInfo> getDomainUpdatePlanList() {
        return domainUpdatePlanList;
    }

    public void setDomainUpdatePlanList(List<DomainUpdatePlanInfo> domainUpdatePlanList) {
        this.domainUpdatePlanList = domainUpdatePlanList;
    }

    public PlatformUpdateTaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(PlatformUpdateTaskType taskType) {
        this.taskType = taskType;
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

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getBkBizId() {
        return bkBizId;
    }

    public void setBkBizId(int bkBizId) {
        this.bkBizId = bkBizId;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }
}
