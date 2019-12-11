package com.channelsoft.ccod.support.cmdb.po;

import java.util.Date;

/**
 * @ClassName: DomainUpdatePlanPo
 * @Author: lanhb
 * @Description: 用来定义域升级方案的pojo类
 * @Date: 2019/12/11 9:30
 * @Version: 1.0
 */
public class DomainUpdatePlanPo {

    private int planId;   //域升级方案id,由数据库唯一生成

    private int schemaId; //该域升级方案归属于哪个平台升级计划,外键平台升级计划表

    private int updateType; //该域升级方案类型,由DomainUpdateType枚举定义

    private int status; //该升级方案当前状态,由DomainUpdateStatus枚举定义

    private Date createTime; //该方案创建时间

    private Date updateTime; //方案最后一次修改时间

    private Date executeTime; //计划执行时间

    private Date startTime; //开始执行计划时间

    private Date endTime; //计划执行结束时间

    private String comment; //备注

    public int getPlanId() {
        return planId;
    }

    public void setPlanId(int planId) {
        this.planId = planId;
    }

    public int getSchemaId() {
        return schemaId;
    }

    public void setSchemaId(int schemaId) {
        this.schemaId = schemaId;
    }

    public int getUpdateType() {
        return updateType;
    }

    public void setUpdateType(int updateType) {
        this.updateType = updateType;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
}
