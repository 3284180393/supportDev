package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.CCODPlatformStatus;
import com.channelsoft.ccod.support.cmdb.po.DomainPo;
import com.channelsoft.ccod.support.cmdb.po.PlatformPo;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: PlatformTopologyInfo
 * @Author: lanhb
 * @Description: 用来定义平台拓扑信息
 * @Date: 2019/12/25 17:52
 * @Version: 1.0
 */
public class PlatformTopologyInfo {

    private String platformId; //平台id

    private String platformName; //平台名,同时对应蓝鲸paas的biz name

    private int bkBizId; //该平台在蓝鲸paas的biz id

    private int bkCloudId; //该平台下面所有host所在的cloud id

    private CCODPlatformStatus status; //该平台的状态

    private List<CCODSetInfo> setList; //平台下的所有集群

    private List<CCODHostInfo> idleHostList; //平台下的所有空闲服务器列表

    private PlatformUpdateSchemaInfo schema;  //平台的升级计划

    public PlatformTopologyInfo(PlatformPo platform)
    {
        this.platformId = platform.getPlatformId();
        this.platformName = platform.getPlatformName();
        this.bkBizId = platform.getBkBizId();
        this.bkCloudId = platform.getBkCloudId();
        this.status = CCODPlatformStatus.getEnumById(platform.getStatus());
        this.setList = new ArrayList<>();
        this.idleHostList = new ArrayList<>();
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public int getBkBizId() {
        return bkBizId;
    }

    public void setBkBizId(int bkBizId) {
        this.bkBizId = bkBizId;
    }

    public int getBkCloudId() {
        return bkCloudId;
    }

    public void setBkCloudId(int bkCloudId) {
        this.bkCloudId = bkCloudId;
    }

    public CCODPlatformStatus getStatus() {
        return status;
    }

    public void setStatus(CCODPlatformStatus status) {
        this.status = status;
    }

    public List<CCODSetInfo> getSetList() {
        return setList;
    }

    public void setSetList(List<CCODSetInfo> setList) {
        this.setList = setList;
    }

    public List<CCODHostInfo> getIdleHostList() {
        return idleHostList;
    }

    public void setIdleHostList(List<CCODHostInfo> idleHostList) {
        this.idleHostList = idleHostList;
    }

    public PlatformUpdateSchemaInfo getSchema() {
        return schema;
    }

    public void setSchema(PlatformUpdateSchemaInfo schema) {
        this.schema = schema;
    }
}
