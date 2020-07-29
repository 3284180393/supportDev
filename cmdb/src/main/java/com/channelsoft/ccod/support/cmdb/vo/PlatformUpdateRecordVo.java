package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.po.PlatformUpdateRecordPo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: PlatformUpdateRecordVo
 * @Author: lanhb
 * @Description: 用来展示平台升级记录的类
 * @Date: 2020/7/28 13:48
 * @Version: 1.0
 */
public class PlatformUpdateRecordVo {

    public PlatformUpdateRecordVo(PlatformUpdateRecordPo recordPo, Gson gson)
    {
        this.recordId = recordPo.getRecordId();
        this.platformId = recordPo.getPlatformId();
        this.jobId = recordPo.getJobId();
        this.preUpdateJobId = recordPo.getPreUpdateJobId();
        this.preDeployApps = gson.fromJson(new String(recordPo.getPreDeployApps()), new TypeToken<List<PlatformAppDeployDetailVo>>() {}.getType());
        this.execSchema = gson.fromJson(new String(recordPo.getExecSchema()), PlatformUpdateSchemaInfo.class);
        this.updateTime = recordPo.getUpdateTime();
        this.timeUsage = recordPo.getTimeUsage();
        this.result = recordPo.isResult();
        this.comment = recordPo.getComment();
        this.isLast = recordPo.isLast();
    }

    private int recordId;

    private String platformId;

    private String jobId;

    private String preUpdateJobId;

    private List<PlatformAppDeployDetailVo> preDeployApps;

    private PlatformUpdateSchemaInfo execSchema;

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

    public String getPreUpdateJobId() {
        return preUpdateJobId;
    }

    public void setPreUpdateJobId(String preUpdateJobId) {
        this.preUpdateJobId = preUpdateJobId;
    }

    public List<PlatformAppDeployDetailVo> getPreDeployApps() {
        return preDeployApps;
    }

    public void setPreDeployApps(List<PlatformAppDeployDetailVo> preDeployApps) {
        this.preDeployApps = preDeployApps;
    }

    public PlatformUpdateSchemaInfo getExecSchema() {
        return execSchema;
    }

    public void setExecSchema(PlatformUpdateSchemaInfo execSchema) {
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

    public boolean isLast() {
        return isLast;
    }

    public void setLast(boolean last) {
        isLast = last;
    }
}
