package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.constant.AppUpdateOperation;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @ClassName: AppK8sDeployInfo
 * @Author: lanhb
 * @Description: 用来定义同应用k8s部署相关的信息
 * @Date: 2020/8/12 17:24
 * @Version: 1.0
 */
public class AppK8sDeployInfo {

    private AppType appType;  //应用类型

    private String appName; //应用名

    private String alias; //应用别名

    private AppUpdateOperation operation; //操作 ADD、UPDATE、DELETE、DEBUG

    private AppUpdateOperationInfo optInfo; //如果类型是ccod应用，该应用部署相关明细

    private List<K8sOperationInfo> steps; //k8s的执行步骤

    private boolean restartGls; //是否需要重启gls server

    private String updateGlsSql; //如果需要重启gls server更新gls server库的sql

    public AppK8sDeployInfo(AppType appType, String appName, String alias, AppUpdateOperation operation, List<K8sOperationInfo> steps)
    {
        this.appType = appType;
        this.appName = appName;
        this.alias = alias;
        this.operation = operation;
        this.steps = steps;
        this.restartGls = false;
        this.updateGlsSql = null;
    }

    public AppType getAppType() {
        return appType;
    }

    public void setAppType(AppType appType) {
        this.appType = appType;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public AppUpdateOperation getOperation() {
        return operation;
    }

    public void setOperation(AppUpdateOperation operation) {
        this.operation = operation;
    }

    public AppUpdateOperationInfo getOptInfo() {
        return optInfo;
    }

    public void setOptInfo(AppUpdateOperationInfo optInfo) {
        this.optInfo = optInfo;
    }

    public List<K8sOperationInfo> getSteps() {
        return steps;
    }

    public void setSteps(List<K8sOperationInfo> steps) {
        this.steps = steps;
    }

    public boolean isRestartGls() {
        return restartGls;
    }

    public void setRestartGls(boolean restartGls) {
        this.restartGls = restartGls;
    }

    public String getUpdateGlsSql() {
        return updateGlsSql;
    }

    public void setUpdateGlsSql(String updateGlsSql) {
        if(StringUtils.isBlank(updateGlsSql)) {
            this.updateGlsSql = null;
            this.restartGls = false;
        }
        else{
            this.updateGlsSql = updateGlsSql;
            this.restartGls = true;
        }
    }
}
