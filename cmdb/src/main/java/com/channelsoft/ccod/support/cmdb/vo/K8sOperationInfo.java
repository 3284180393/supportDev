package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.K8sKind;
import com.channelsoft.ccod.support.cmdb.constant.K8sOperation;
import org.springframework.beans.factory.annotation.Autowired;

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

    private boolean kernal; //是否是核心模块，如果是核心模块，则必须CREATE/REPLACE该模块的deployment成功后才执行后面的操作

    private int timeout; //如果kernal为true，则timeout秒内该命令未能执行成功将返回false

    public K8sOperationInfo(String jobId, String platformId, String domainId, K8sKind kind, String name, K8sOperation operation, Object obj)
    {
        this.jobId = jobId;
        this.platformId = platformId;
        this.domainId = domainId;
        this.kind = kind;
        this.name = name;
        this.operation = operation;
        this.obj = obj;
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

    public boolean isKernal() {
        return kernal;
    }

    public void setKernal(boolean kernal) {
        this.kernal = kernal;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
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
