package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.K8sKind;
import com.channelsoft.ccod.support.cmdb.constant.K8sOperation;

/**
 * @ClassName: K8sOperationInfo
 * @Author: lanhb
 * @Description: 用来定义k8s操作信息
 * @Date: 2020/7/17 10:42
 * @Version: 1.0
 */
public class K8sOperationInfo {

    private String platformId; //平台id

    private String domainId; //域id

    private String jobId; //执行此次操作的job id

    private K8sKind kind; //操作的k8s对象类型

    private String name; //操作对象的名称

    private K8sOperation operation; //操作类型

    private Object obj; //该操作对象

    private boolean updateGlsServer; //是否需要更新glsserver

    private String updateSql; //更新glsserver的sql

    public K8sOperationInfo(String jobId, String platformId, String domainId, K8sKind kind, String name, K8sOperation operation, Object obj)
    {
        this.jobId = jobId;
        this.platformId = platformId;
        this.domainId = domainId;
        this.kind = kind;
        this.name = name;
        this.operation = operation;
        this.obj = obj;
        this.updateGlsServer = false;
        this.updateSql = null;
    }

    public K8sOperationInfo(K8sKind kind, String name, K8sOperation operation, Object obj)
    {
        this.kind = kind;
        this.name = name;
        this.operation = operation;
        this.obj = obj;
        this.updateGlsServer = false;
        this.updateSql = null;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public K8sOperation getOperation() {
        return operation;
    }

    public void setOperation(K8sOperation operation) {
        this.operation = operation;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public boolean isUpdateGlsServer() {
        return updateGlsServer;
    }

    public void setUpdateGlsServer(boolean updateGlsServer) {
        this.updateGlsServer = updateGlsServer;
    }

    public String getUpdateSql() {
        return updateSql;
    }

    public void setUpdateSql(String updateSql) {
        this.updateSql = updateSql;
    }
}
