package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.constant.K8sKind;
import com.channelsoft.ccod.support.cmdb.constant.K8sOperation;

/**
 * @ClassName: K8sOperationPo
 * @Author: lanhb
 * @Description: 用来定义k8s操作详情的pojo类型
 * @Date: 2020/7/11 17:38
 * @Version: 1.0
 */
public class K8sOperationPo {

    private int operationId; //id，数据库唯一主键

    private String platformId; //平台id

    private String domainId; //域id

    private String jobId; //执行此次操作的job id

    private K8sKind kind; //操作的k8s对象类型

    private String name; //操作对象的名称

    private K8sOperation operation; //操作类型

    private String srcJson; //该对象操作之前的json

    private String dstJson; //该对象操作后的json

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

    public String getSrcJson() {
        return srcJson;
    }

    public void setSrcJson(String srcJson) {
        this.srcJson = srcJson;
    }

    public String getDstJson() {
        return dstJson;
    }

    public void setDstJson(String dstJson) {
        this.dstJson = dstJson;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
