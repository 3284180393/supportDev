package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.constant.K8sKind;
import com.channelsoft.ccod.support.cmdb.constant.K8sOperation;

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

    private String comment; //如果执行失败了，失败说明

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
}
