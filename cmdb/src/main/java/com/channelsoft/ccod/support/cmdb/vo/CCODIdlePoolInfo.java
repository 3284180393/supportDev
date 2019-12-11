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
    private int bkBizId;  //资源池对应的biz id

    private int bkSetId; //资源池对应的set id

    private String setId; //资源池在cmdb中的id,方便查询

    private String setName; //资源池对应的set名

    private List<CCODHostInfo> idleHosts; //资源池拥有的空闲host

    public CCODIdlePoolInfo(int bkBizId, LJSetInfo idlePoolSet, List<LJHostInfo> idleHosts)
    {
        this.bkBizId = bkBizId;
        this.bkSetId = idlePoolSet.getSetId();
        this.setName = idlePoolSet.getSetName();
        this.idleHosts = new ArrayList<>();
        for(LJHostInfo host : idleHosts)
        {
            CCODHostInfo idleHost = new CCODHostInfo(host);
            this.idleHosts.add(idleHost);
        }
    }

    public CCODIdlePoolInfo(LJSetInfo idlePoolSet)
    {
        this.bkBizId = idlePoolSet.getBizId();
        this.bkSetId = idlePoolSet.getSetId();
        this.setName = idlePoolSet.getSetName();
        this.idleHosts = new ArrayList<>();
        this.setId = idlePoolSet.getSetName();
    }

    public int getBkBizId() {
        return bkBizId;
    }

    public void setBkBizId(int bkBizId) {
        this.bkBizId = bkBizId;
    }

    public int getBkSetId() {
        return bkSetId;
    }

    public void setBkSetId(int bkSetId) {
        this.bkSetId = bkSetId;
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

    public String getSetId() {
        return setId;
    }

    public void setSetId(String setId) {
        this.setId = setId;
    }

    public LJSetInfo getIdlePoolSet()
    {
        LJSetInfo setInfo = new LJSetInfo();
        setInfo.setBizId(this.bkBizId);
        setInfo.setSetName(this.setName);
        setInfo.setSetId(this.bkSetId);
        return setInfo;
    }
}
