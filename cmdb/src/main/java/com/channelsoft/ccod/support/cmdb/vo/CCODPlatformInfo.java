package com.channelsoft.ccod.support.cmdb.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: CCODPlatformInfo
 * @Author: lanhb
 * @Description: 用来定义ccod的平台信息
 * @Date: 2019/12/3 17:23
 * @Version: 1.0
 */
public class CCODPlatformInfo {
    private String platformName; //平台名,同时对应蓝鲸paas的biz name

    private int platId; //该平台在数据库的组件

    private int bizId; //该平台在蓝鲸paas的biz id

    private String platformId; //平台id

    private int status; //该平台的状态

    private List<CCODSetInfo> sets; //该平台下除开idle pools外所有set

    private CCODIdlePoolInfo idlePool; //该平台的idle pools set

    private CCODDomainInfo planNewDomain; //计划新建的域,对应的status为9

    private List<CCODModuleInfo> planUpdateApps; //计划更新的模块,对应的status为10

    public CCODPlatformInfo()
    {
        this.sets = new ArrayList<>();
        this.planUpdateApps = new ArrayList<>();
    }

    public CCODPlatformInfo(LJBizInfo bizInfo, int status)
    {
        this.bizId = bizInfo.getBizId();
        this.platformName = bizInfo.getBizName();
        this.status = status;
        this.sets = new ArrayList<>();
        this.planUpdateApps = new ArrayList<>();
    }


    public CCODPlatformInfo(LJBizInfo bizInfo, int status, CCODIdlePoolInfo idlePool, List<CCODSetInfo> sets)
    {
        this.bizId = bizInfo.getBizId();
        this.platformName = bizInfo.getBizName();
        this.status = status;
        this.idlePool = idlePool;
        this.sets = sets;
        this.planUpdateApps = new ArrayList<>();
    }

    public int getBizId() {
        return bizId;
    }

    public void setBizId(int bizId) {
        this.bizId = bizId;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public int getPlatId() {
        return platId;
    }

    public void setPlatId(int platId) {
        this.platId = platId;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<CCODSetInfo> getSets() {
        return sets;
    }

    public void setSets(List<CCODSetInfo> sets) {
        this.sets = sets;
    }

    public CCODIdlePoolInfo getIdlePool() {
        return idlePool;
    }

    public void setIdlePool(CCODIdlePoolInfo idlePool) {
        this.idlePool = idlePool;
    }

    public CCODDomainInfo getPlanNewDomain() {
        return planNewDomain;
    }

    public void setPlanNewDomain(CCODDomainInfo planNewDomain) {
        this.planNewDomain = planNewDomain;
    }

    public List<CCODModuleInfo> getPlanUpdateApps() {
        return planUpdateApps;
    }

    public void setPlanUpdateApps(List<CCODModuleInfo> planUpdateApps) {
        this.planUpdateApps = planUpdateApps;
    }

    @Override
    public CCODPlatformInfo clone()
    {
        CCODPlatformInfo platformInfo = new CCODPlatformInfo();
        platformInfo.status = this.status;
        platformInfo.platformName = this.platformName;
        platformInfo.bizId = this.bizId;
        platformInfo.platformId = this.platformId;
        platformInfo.platId = this.platId;
        List<CCODSetInfo> setList = new ArrayList<>();
        for(CCODSetInfo set : this.sets)
        {
            setList.add(set.clone());
        }
        platformInfo.sets = setList;
        platformInfo.idlePool = this.idlePool;
        if(planNewDomain != null)
        {
            platformInfo.planNewDomain = this.planNewDomain.clone();
        }
        List<CCODModuleInfo> updateList = new ArrayList<>();
        for(CCODModuleInfo module : this.planUpdateApps)
        {
            updateList.add(module.clone());
        }
        platformInfo.planUpdateApps = updateList;
        return platformInfo;
    }
}
