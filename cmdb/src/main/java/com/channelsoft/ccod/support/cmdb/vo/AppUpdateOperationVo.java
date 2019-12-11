package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.AppUpdateOperation;
import com.channelsoft.ccod.support.cmdb.constant.AppUpdateOperationStatus;
import com.channelsoft.ccod.support.cmdb.po.AppPo;

import java.util.Date;

/**
 * @ClassName: AppUpdateOperationVo
 * @Author: lanhb
 * @Description: 用来定义应用升级详细信息的类
 * @Date: 2019/12/11 14:41
 * @Version: 1.0
 */
public class AppUpdateOperationVo {

    private int operationId;  //应用操作id,由数据库唯一生成

    private int bkAppModuleId; //如果a操作为修改或是删除app,此id为app对应蓝鲸paas平台的bkModuleId,否则为0

    private int domainPlanId; //该操作对应的域升级方案id

    private int platformSchemaId; //该操作对应的平台升级计划id

    private AppUpdateOperation operation; //将要执行的操作

    private AppPo originalApp; //原始app,如果操作为新增则为空

    private AppPo targetApp; //目标app,如果操作为删除或是修改配置文件则为空

    private LJHostInfo host; //被执行操作的应用所在的服务器信息

    private String appBasePath; //应用的base path

    private String appRunner; //应用的运行用户

    private Date createTime; //创建时间

    private Date executeTime; //该升级操作的执行时间

    private AppUpdateOperationStatus status; //当前状态

    private String comment; //备注

    public int getOperationId() {
        return operationId;
    }

    public void setOperationId(int operationId) {
        this.operationId = operationId;
    }

    public int getBkAppModuleId() {
        return bkAppModuleId;
    }

    public void setBkAppModuleId(int bkAppModuleId) {
        this.bkAppModuleId = bkAppModuleId;
    }

    public int getDomainPlanId() {
        return domainPlanId;
    }

    public void setDomainPlanId(int domainPlanId) {
        this.domainPlanId = domainPlanId;
    }

    public int getPlatformSchemaId() {
        return platformSchemaId;
    }

    public void setPlatformSchemaId(int platformSchemaId) {
        this.platformSchemaId = platformSchemaId;
    }

    public AppUpdateOperation getOperation() {
        return operation;
    }

    public void setOperation(AppUpdateOperation operation) {
        this.operation = operation;
    }

    public AppPo getOriginalApp() {
        return originalApp;
    }

    public void setOriginalApp(AppPo originalApp) {
        this.originalApp = originalApp;
    }

    public AppPo getTargetApp() {
        return targetApp;
    }

    public void setTargetApp(AppPo targetApp) {
        this.targetApp = targetApp;
    }

    public LJHostInfo getHost() {
        return host;
    }

    public void setHost(LJHostInfo host) {
        this.host = host;
    }

    public String getAppBasePath() {
        return appBasePath;
    }

    public void setAppBasePath(String appBasePath) {
        this.appBasePath = appBasePath;
    }

    public String getAppRunner() {
        return appRunner;
    }

    public void setAppRunner(String appRunner) {
        this.appRunner = appRunner;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(Date executeTime) {
        this.executeTime = executeTime;
    }

    public AppUpdateOperationStatus getStatus() {
        return status;
    }

    public void setStatus(AppUpdateOperationStatus status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
