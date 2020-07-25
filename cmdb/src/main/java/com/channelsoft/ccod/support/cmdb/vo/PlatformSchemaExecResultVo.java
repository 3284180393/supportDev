package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.po.K8sOperationPo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @ClassName: PlatformSchemaExecResultVo
 * @Author: lanhb
 * @Description: 用来定义平台规划执行结果的类
 * @Date: 2020/7/25 14:44
 * @Version: 1.0
 */
public class PlatformSchemaExecResultVo {

    private String jobId; //升级任务的job id

    private String platformId; //升级平台

    private List<K8sOperationInfo> execSteps; //执行步骤

    private Date startTime; //开始执行时间

    private Date endTime; //结束执行时间

    private int timeUsage; //用时

    private List<K8sOperationPo> execResults; //执行结果

    private boolean isSuccess; //是否执行成功

    private String errorMsg; //如果执行失败错误信息

    private List<K8sOperationInfo> rollbackSteps; //如果执行失败后的回滚步骤

    private List<K8sOperationPo> rollbackResults; //回滚结果

    private boolean isRollbackSuccess; //是否回滚成功

    private String rollBackFailReason; //回滚失败原因

    public PlatformSchemaExecResultVo(String jobId, String platformId, List<K8sOperationInfo> execSteps)
    {
        this.jobId = jobId;
        this.platformId = platformId;
        this.execSteps = execSteps;
        this.startTime = new Date();
        this.execResults = new ArrayList<>();
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public List<K8sOperationInfo> getExecSteps() {
        return execSteps;
    }

    public void setExecSteps(List<K8sOperationInfo> execSteps) {
        this.execSteps = execSteps;
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

    public List<K8sOperationPo> getExecResults() {
        return execResults;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public List<K8sOperationInfo> getRollbackSteps() {
        return rollbackSteps;
    }

    public void setRollbackSteps(List<K8sOperationInfo> rollbackSteps) {
        this.rollbackSteps = rollbackSteps;
    }

    public List<K8sOperationPo> getRollbackResults() {
        return rollbackResults;
    }

    public void setRollbackResults(List<K8sOperationPo> rollbackResults) {
        this.rollbackResults = rollbackResults;
    }

    public boolean isRollbackSuccess() {
        return isRollbackSuccess;
    }

    public void setRollbackSuccess(boolean rollbackSuccess) {
        isRollbackSuccess = rollbackSuccess;
    }

    public String getRollBackFailReason() {
        return rollBackFailReason;
    }

    public void setRollBackFailReason(String rollBackFailReason) {
        this.rollBackFailReason = rollBackFailReason;
    }

    public int getTimeUsage() {
        return timeUsage;
    }

    public void execSuccess(List<K8sOperationPo> execResults)
    {
        this.isSuccess = true;
        this.execResults = execResults;
        this.endTime = new Date();
        this.timeUsage = (int)((this.endTime.getTime() - this.startTime.getTime())/1000);
    }

    public void execFail(List<K8sOperationPo> execResults, String errorMsg)
    {
        this.isSuccess = false;
        this.execResults = execResults;
        this.errorMsg = errorMsg;
        this.endTime = new Date();
        this.timeUsage = (int)((this.endTime.getTime() - this.startTime.getTime())/1000);
    }
}
