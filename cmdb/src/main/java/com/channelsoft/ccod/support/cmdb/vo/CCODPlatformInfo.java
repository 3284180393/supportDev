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

    private UpdatePlatformSchemaVo updateSchema; //正在进行的平台升级计划,如果没有为空

    public CCODPlatformInfo()
    {
        this.sets = new ArrayList<>();
    }

    public CCODPlatformInfo(LJBizInfo bizInfo, int status)
    {
        this.bizId = bizInfo.getBizId();
        this.platformName = bizInfo.getBizName();
        this.status = status;
        this.sets = new ArrayList<>();
    }


    public CCODPlatformInfo(LJBizInfo bizInfo, int status, CCODIdlePoolInfo idlePool, List<CCODSetInfo> sets)
    {
        this.bizId = bizInfo.getBizId();
        this.platformName = bizInfo.getBizName();
        this.status = status;
        this.idlePool = idlePool;
        this.sets = sets;
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
        return platformInfo;
    }
}
