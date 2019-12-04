package com.channelsoft.ccod.support.cmdb.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: CCODIdlePoolInfo
 * @Author: lanhb
 * @Description: 用来定义ccod的空闲池信息, 对应于蓝鲸paas平台的idle pools概念
 * @Date: 2019/12/4 9:27
 * @Version: 1.0
 */
public class CCODIdlePoolInfo {
    private int bizId;  //资源池对应的biz id

    private int setId; //资源池对应的set id

    private String setName; //资源池对应的set名

    private List<CCODHostInfo> idleHosts; //资源池拥有的空闲host

    public CCODIdlePoolInfo(int bizId, int setId, String setName)
    {
        this.bizId = bizId;
        this.setId = setId;
        this.setName = setName;
        this.idleHosts = new ArrayList<>();
    }

    public int getBizId() {
        return bizId;
    }

    public void setBizId(int bizId) {
        this.bizId = bizId;
    }

    public int getSetId() {
        return setId;
    }

    public void setSetId(int setId) {
        this.setId = setId;
    }

    public String getSetName() {
        return setName;
    }

    public void setSetName(String setName) {
        this.setName = setName;
    }

    public List<CCODHostInfo> getIdleHosts() {
        return idleHosts;
    }

    public void setIdleHosts(List<CCODHostInfo> idleHosts) {
        this.idleHosts = idleHosts;
    }
}
