package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.constant.K8sKind;
import com.channelsoft.ccod.support.cmdb.constant.K8sOperation;
import com.channelsoft.ccod.support.cmdb.vo.K8sOperationInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * @ClassName: K8sOperationPo
 * @Author: lanhb
 * @Description: 用来定义k8s操作详情的pojo类型
 * @Date: 2020/7/11 17:38
 * @Version: 1.0
 */
public class K8sOperationPo {

    private int operationId; //id，数据库唯一主键

    private String jobId; //执行此次操作的job id

    private String platformId; //平台id

    private String domainId; //域id

    private K8sKind kind; //操作的k8s对象类型

    private String name; //操作对象的名称

    private K8sOperation operation; //操作类型

    private String json; //操作对象的json内容

    private Date startTime; //开始执行时间

    private Date endTime; //结束执行时间

    private long timeUsage; //用时

    private boolean isSuccess; //是否执行成功

    private String retJson; //如果执行该项操作需要返回结果，返回结果的json

    private String comment; //如果执行失败了，失败说明

    private String desc; //操作描述

    public K8sOperationPo() {}

    public K8sOperationPo(String jobId, String platformId, String domainId, K8sKind kind, String name, K8sOperation operation, String json)
    {
        this.jobId = jobId;
        this.platformId = platformId;
        this.domainId = domainId;
        this.kind = kind;
        this.name = name;
        this.operation = operation;
        this.json = json;
        this.startTime = new Date();
        if(StringUtils.isBlank(platformId))
            this.desc = String.format("%s %s %s to k8s", operation.name, kind.name, name);
        else if(StringUtils.isBlank(domainId))
            this.desc = String.format("%s %s %s to namespace %s", operation.name, kind.name, name, platformId, domainId);
        else
            desc = String.format("%s %s %s to namespace %s for domain %s", operation.name, kind.name, name, platformId, domainId);
    }

    public int getOperationId() {
        return operationId;
    }

    public void setOperationId(int operationId) {
        this.operationId = operationId;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public K8sKind getKind() {
        return kind;
    }

    public void setKind(K8sKind kind) {
        this.kind = kind;
    }

    public K8sOperation getOperation() {
        return operation;
    }

    public void setOperation(K8sOperation operation) {
        this.operation = operation;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
        endTime = new Date();
        timeUsage = endTime.getTime() - startTime.getTime();
    }

    public String getRetJson() {
        return retJson;
    }

    public void setRetJson(String retJson) {
        this.retJson = retJson;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getTimeUsage() {
        return timeUsage;
    }

    public void setTimeUsage(long timeUsage) {
        this.timeUsage = timeUsage;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void success(String retJson)
    {
        this.isSuccess = true;
        this.retJson = retJson;
        this.comment = "execute success";
        this.endTime = new Date();
        this.timeUsage = endTime.getTime() - startTime.getTime();
        this.desc = String.format("%s : success, timeUsage=%d", this.desc, this.timeUsage);
    }

    public void fail(String errMsg)
    {
        this.isSuccess = false;
        this.comment = errMsg;
        this.endTime = new Date();
        this.timeUsage = endTime.getTime() - startTime.getTime();
        this.desc = String.format("%s fail : %s, timeUsage=%d(ms)", this.desc, errMsg, this.timeUsage);
    }

    @Autowired
    public String toString()
    {
        if(platformId == null)
            return String.format("%s %s %s to k8s", operation.name, kind.name, name);
        else if(domainId == null)
            return String.format("%s %s %s to namespace %s", operation.name, kind.name, name, platformId);
        else
            return String.format("%s %s %s to namespace %s for %s", operation.name, kind.name, name, platformId, domainId);
    }
}
