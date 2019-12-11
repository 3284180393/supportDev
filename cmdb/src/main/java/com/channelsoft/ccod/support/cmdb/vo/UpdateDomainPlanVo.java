package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.DomainUpdateStatus;
import com.channelsoft.ccod.support.cmdb.constant.DomainUpdateType;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: UpdateDomainPlanVo
 * @Author: lanhb
 * @Description: 用来定义升级域的方案类
 * @Date: 2019/12/11 15:17
 * @Version: 1.0
 */
public class UpdateDomainPlanVo {

    private int planId; //该方案的id,由数据库唯一生成

    private int schemaId; //该方案归属的平台升级计划id

    private int domId; //执行该方案的域的domId

    private String domainName; //执行该方案的域名

    private String domainId; //执行该方案的域id

    private DomainUpdateType updateType; //升级类型

    private DomainUpdateStatus updateStatus; //当前升级状态

    private List<AppUpdateOperationVo> appUpdateOperationList; //升级需要执行的操作列表

    private Date createTime; //该方案创建时间

    private Date updateTime; //方案最后一次修改时间

    private Date executeTime; //计划执行时间

    private Date startTime; //开始执行计划时间

    private Date endTime; //计划执行结束时间

    private String comment; //备注

    public int getPlanId() {
        return planId;
    }

    public void setPlanId(int planId) {
        this.planId = planId;
    }

    public int getSchemaId() {
        return schemaId;
    }

    public void setSchemaId(int schemaId) {
        this.schemaId = schemaId;
    }

    public int getDomId() {
        return domId;
    }

    public void setDomId(int domId) {
        this.domId = domId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public DomainUpdateType getUpdateType() {
        return updateType;
    }

    public void setUpdateType(DomainUpdateType updateType) {
        this.updateType = updateType;
    }

    public DomainUpdateStatus getUpdateStatus() {
        return updateStatus;
    }

    public void setUpdateStatus(DomainUpdateStatus updateStatus) {
        this.updateStatus = updateStatus;
    }

    public List<AppUpdateOperationVo> getAppUpdateOperationList() {
        return appUpdateOperationList;
    }

    public void setAppUpdateOperationList(List<AppUpdateOperationVo> appUpdateOperationList) {
        this.appUpdateOperationList = appUpdateOperationList;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Date getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(Date executeTime) {
        this.executeTime = executeTime;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
