package com.channelsoft.ccod.support.cmdb.po;

import java.util.Date;

/**
 * @ClassName: PlatformUpdateRecordPo
 * @Author: lanhb
 * @Description: 用来记录平台升级结果的pojo类
 * @Date: 2020/7/27 15:22
 * @Version: 1.0
 */
public class PlatformUpdateRecordPo {
    private int recordId;

    private String platformId;

    private String jobId;

    private String preUpdateJobId;

    private byte[] preDeployApps;

    private byte[] execSchema;

    private Date updateTime;

    private int timeUsage;

    private boolean result;

    private String comment;

    private boolean isLast;

    public int getRecordId() {
        return recordId;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public byte[] getExecSchema() {
        return execSchema;
    }

    public void setExecSchema(byte[] execSchema) {
        this.execSchema = execSchema;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public int getTimeUsage() {
        return timeUsage;
    }

    public void setTimeUsage(int timeUsage) {
        this.timeUsage = timeUsage;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public byte[] getPreDeployApps() {
        return preDeployApps;
    }

    public void setPreDeployApps(byte[] preDeployApps) {
        this.preDeployApps = preDeployApps;
    }

    public String getPreUpdateJobId() {
        return preUpdateJobId;
    }

    public void setPreUpdateJobId(String preUpdateJobId) {
        this.preUpdateJobId = preUpdateJobId;
    }

    public boolean isLast() {
        return isLast;
    }

    public void setLast(boolean last) {
        isLast = last;
    }
}
