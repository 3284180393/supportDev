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

    private int setId; //资源池对应的蓝鲸paas set id

    private String setName; //资源池对应的蓝鲸 paas set名

    private List<LJHostInfo> idleHosts; //资源池拥有的空闲主机信息

    public LJIdlePoolInfo(int bizId, int setId, String setName)
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

    public List<LJHostInfo> getIdleHosts() {
        return idleHosts;
    }

    public void setIdleHosts(List<LJHostInfo> idleHosts) {
        this.idleHosts = idleHosts;
    }
}
