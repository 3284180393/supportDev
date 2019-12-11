package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.PlatformUpdateTaskStatus;
import com.channelsoft.ccod.support.cmdb.constant.PlatformUpdateTaskType;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: UpdatePlatformSchemaVo
 * @Author: lanhb
 * @Description: 用来定义升级平台的计划类
 * @Date: 2019/12/11 16:08
 * @Version: 1.0
 */
public class UpdatePlatformSchemaVo {

    private int schemaId; //schemaId

    private String platId; //平台id

    private String platformName; //平台名

    private String platformId; //平台状态

    private PlatformUpdateTaskType taskType; //平台升级任务类型

    private PlatformUpdateTaskStatus taskStatus; //平台升级任务状态

    private List<UpdateDomainPlanVo> updateDomainPlanList; //域升级计划列表

    private Date createTime; //计划创建时间

    private Date updateTime; //计划最后一次修改时间

    private Date startTime; //开始执行计划时间

    private Date endTime; //任务结束时间

    private Date executeTime; //如果该时间为非空，在该时间自动执行

    private Date deadline; //计划完成最后期限

    private String title; //升级任务标题

    private String comment; //备注

    public int getSchemaId() {
        return schemaId;
    }

    public void setSchemaId(int schemaId) {
        this.schemaId = schemaId;
    }

    public String getPlatId() {
        return platId;
    }

    public void setPlatId(String platId) {
        this.platId = platId;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public PlatformUpdateTaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(PlatformUpdateTaskType taskType) {
        this.taskType = taskType;
    }

    public PlatformUpdateTaskStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(PlatformUpdateTaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    public List<UpdateDomainPlanVo> getUpdateDomainPlanList() {
        return updateDomainPlanList;
    }

    public void setUpdateDomainPlanList(List<UpdateDomainPlanVo> updateDomainPlanList) {
        this.updateDomainPlanList = updateDomainPlanList;
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

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
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
}
