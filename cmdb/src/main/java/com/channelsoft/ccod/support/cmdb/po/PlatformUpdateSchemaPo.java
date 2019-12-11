package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.constant.PlatformUpdateTaskType;
import com.channelsoft.ccod.support.cmdb.constant.UpdateStatus;

import java.util.Date;

/**
 * @ClassName: PlatformUpdateSchemaPo
 * @Author: lanhb
 * @Description: 用来定义平台升级计划的pojo类
 * @Date: 2019/12/11 9:13
 * @Version: 1.0
 */
public class PlatformUpdateSchemaPo {

    private PlatformUpdateTaskType taskType; //升级计划的任务类型,由PlatformUpdateTaskType枚举定义

    private UpdateStatus status; //任务当前状态,由PlatformUpdateTaskStatus枚举定义

    private Date createTime; //计划创建时间

    private Date updateTime; //计划最后一次修改时间

    private Date executeTime; //如果该时间为非空，在该时间自动执行

    private Date deadline; //计划完成最后期限

    private String title; //升级任务标题

    private String comment; //备注

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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public Date getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(Date executeTime) {
        this.executeTime = executeTime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
