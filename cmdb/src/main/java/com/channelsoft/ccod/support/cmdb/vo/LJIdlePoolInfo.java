package com.channelsoft.ccod.support.cmdb.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: LJIdlePoolInfo
 * @Author: lanhb
 * @Description: 用来定义蓝鲸空闲资源池信息
 * @Date: 2019/12/4 9:10
 * @Version: 1.0
 */
public class LJIdlePoolInfo {
    private int bizId;   //资源池所属的蓝鲸 paas biz id

    private int bkSetId; //资源池对应的蓝鲸paas set id

    private String setId; //对应的cmdb中的set id

    private String setName; //资源池对应的蓝鲸 paas set名

    private List<LJHostInfo> idleHosts; //资源池拥有的空闲主机信息

    public LJIdlePoolInfo(LJSetInfo idlePoolSet, String setId)
    {
        this.bizId = idlePoolSet.getBkBizId();
        this.bkSetId = idlePoolSet.getBkSetId();
        this.setName = idlePoolSet.getBkSetName();
        this.setId = setId;
        this.idleHosts = new ArrayList<>();
    }

    public int getBizId() {
        return bizId;
    }

    public void setBizId(int bizId) {
        this.bizId = bizId;
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

    public List<LJHostInfo> getIdleHosts() {
        return idleHosts;
    }

    public void setIdleHosts(List<LJHostInfo> idleHosts) {
        this.idleHosts = idleHosts;
    }

    public String getSetId() {
        return setId;
    }

    public void setSetId(String setId) {
        this.setId = setId;
    }
}
