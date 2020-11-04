package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.po.K8sObjectTemplatePo;
import com.channelsoft.ccod.support.cmdb.po.K8sOperationPo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @ClassName: AppDebugTaskVo
 * @Author: lanhb
 * @Description: 用来定义ccod应用调试任务的类
 * @Date: 2020/11/3 13:49
 * @Version: 1.0
 */
public class AppDebugTaskVo {

    public final static int QUEUE = 1; //排队中

    public final static int RUNNING = 2; //正在被执行

    private int id; //任务id

    private AppUpdateOperationInfo debugInfo;

    private int status; //状态

    private Date queueTime; //进入队列时间

    private Date execTime; //执行部署时间

    private int timeout; //调试超时时长

    private List<K8sOperationInfo> steps; //调试步骤

    private List<K8sOperationPo> execResults; //步骤结果

    private boolean success; //是否调试成功

    private String message; //调试新信息

    public AppDebugTaskVo(int id, AppUpdateOperationInfo debugInfo){
        this.id = id;
        this.debugInfo = debugInfo;
        this.status = QUEUE;
        this.queueTime = new Date();
        this.steps = new ArrayList<>();
        this.execResults = new ArrayList<>();
        this.success = false;
        this.message = "queue to wait debug exec";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public AppUpdateOperationInfo getDebugInfo() {
        return debugInfo;
    }

    public void setDebugInfo(AppUpdateOperationInfo debugInfo) {
        this.debugInfo = debugInfo;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public List<K8sOperationInfo> getSteps() {
        return steps;
    }

    public void setSteps(List<K8sOperationInfo> steps) {
        this.steps = steps;
    }

    public List<K8sOperationPo> getExecResults() {
        return execResults;
    }

    public void setExecResults(List<K8sOperationPo> execResults) {
        this.execResults = execResults;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getQueueTime() {
        return queueTime;
    }

    public void setQueueTime(Date queueTime) {
        this.queueTime = queueTime;
    }

    public Date getExecTime() {
        return execTime;
    }

    public void setExecTime(Date execTime) {
        this.execTime = execTime;
    }

    public String getDebugTag()
    {
        return String.format("%s#%s#%s#%s", debugInfo.getPlatformId(), debugInfo.getDomainId(), debugInfo.getAppName(), debugInfo.getAlias());
    }
}
