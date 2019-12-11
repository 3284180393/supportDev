package com.channelsoft.ccod.support.cmdb.po;

import java.util.Date;

/**
 * @ClassName: UpdateStepPo
 * @Author: lanhb
 * @Description: 用来定义升级步骤的类型
 * @Date: 2019/12/11 10:01
 * @Version: 1.0
 */
public class UpdateStepPo {

    private int stepId;

    private int schemaId; //该升级步骤归属哪个升级计划,外键平台升级计划表外键

    private int planId; //该升级方案归属哪个域升级方案,外键域升级方案表

    private int updateType; //应用升级类型,由AppUpdateType枚举定义

    private int from; //操作前应用id,该id由app表定义

    private int to; //操作后应用id,该id由app表定义

    private int bzHostId; //对该应用所在的服务器id,该id由蓝鲸paas平台唯一生成

    private int bkModuleId; //

    private String basePath; //该应用所在的base path

    private String appRunner; //该应用的执行用户

    private Date updateTime; //升级时间

    private int status; //升级状态

    private String comment; //描述

    public int getSchemaId() {
        return schemaId;
    }

    public void setSchemaId(int schemaId) {
        this.schemaId = schemaId;
    }

    public int getPlanId() {
        return planId;
    }

    public void setPlanId(int planId) {
        this.planId = planId;
    }

    public int getUpdateType() {
        return updateType;
    }

    public void setUpdateType(int updateType) {
        this.updateType = updateType;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public int getBzHostId() {
        return bzHostId;
    }

    public void setBzHostId(int bzHostId) {
        this.bzHostId = bzHostId;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getAppRunner() {
        return appRunner;
    }

    public void setAppRunner(String appRunner) {
        this.appRunner = appRunner;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStepId() {
        return stepId;
    }

    public void setStepId(int stepId) {
        this.stepId = stepId;
    }

    public int getBkModuleId() {
        return bkModuleId;
    }

    public void setBkModuleId(int bkModuleId) {
        this.bkModuleId = bkModuleId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
