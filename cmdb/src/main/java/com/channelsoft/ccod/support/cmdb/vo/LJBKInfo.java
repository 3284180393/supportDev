package com.channelsoft.ccod.support.cmdb.vo;

/**
 * @ClassName: LJBKInfo
 * @Author: lanhb
 * @Description: 用来定义从蓝鲸paas获取的bk信息
 * @Date: 2019/12/2 11:11
 * @Version: 1.0
 */
public class LJBKInfo {
    private int bizId;

    private String bizName;

    private int status; //该平台的状态

    private LJSetInfo idlePools; //空闲资源池

    private LJSetInfo healingSet; //故障自愈

    private LJSetInfo dataSets; //数据服务模块

    private LJSetInfo publicModuleSet; //公共组件

    private LJSetInfo integrationSet; //集成平台

    private LJSetInfo jobSet; //作业平台

    private LJSetInfo cfgSet; //配置平台

    private LJSetInfo controlSet; //管控平台

    private LJBKHostInfo[] idleHost; //空闲服务器

    private LJBKHostInfo[] usedHosts; //已经被使用的服务器

    public int getBizId() {
        return bizId;
    }

    public void setBizId(int bizId) {
        this.bizId = bizId;
    }

    public String getBizName() {
        return bizName;
    }

    public void setBizName(String bizName) {
        this.bizName = bizName;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public LJSetInfo getHealingSet() {
        return healingSet;
    }

    public void setHealingSet(LJSetInfo healingSet) {
        this.healingSet = healingSet;
    }

    public LJSetInfo getDataSets() {
        return dataSets;
    }

    public void setDataSets(LJSetInfo dataSets) {
        this.dataSets = dataSets;
    }

    public LJSetInfo getPublicModuleSet() {
        return publicModuleSet;
    }

    public void setPublicModuleSet(LJSetInfo publicModuleSet) {
        this.publicModuleSet = publicModuleSet;
    }

    public LJSetInfo getIntegrationSet() {
        return integrationSet;
    }

    public void setIntegrationSet(LJSetInfo integrationSet) {
        this.integrationSet = integrationSet;
    }

    public LJSetInfo getJobSet() {
        return jobSet;
    }

    public void setJobSet(LJSetInfo jobSet) {
        this.jobSet = jobSet;
    }

    public LJSetInfo getCfgSet() {
        return cfgSet;
    }

    public void setCfgSet(LJSetInfo cfgSet) {
        this.cfgSet = cfgSet;
    }

    public LJSetInfo getControlSet() {
        return controlSet;
    }

    public void setControlSet(LJSetInfo controlSet) {
        this.controlSet = controlSet;
    }

    public LJBKHostInfo[] getIdleHost() {
        return idleHost;
    }

    public void setIdleHost(LJBKHostInfo[] idleHost) {
        this.idleHost = idleHost;
    }

    public LJBKHostInfo[] getUsedHosts() {
        return usedHosts;
    }

    public void setUsedHosts(LJBKHostInfo[] usedHosts) {
        this.usedHosts = usedHosts;
    }

    public LJSetInfo getIdlePools() {
        return idlePools;
    }

    public void setIdlePools(LJSetInfo idlePools) {
        this.idlePools = idlePools;
    }
}
