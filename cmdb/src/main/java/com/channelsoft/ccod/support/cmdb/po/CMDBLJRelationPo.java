package com.channelsoft.ccod.support.cmdb.po;

/**
 * @ClassName: CMDBLJRelationPo
 * @Author: lanhb
 * @Description: 用来定义cmd和蓝鲸paas关系的类
 * @Date: 2019/12/2 10:11
 * @Version: 1.0
 */
public class CMDBLJRelationPo {

    private int id; //id数据库表唯一主键

    private String platformId; //cmdb的平台id

    private int bkBizId; //蓝鲸paas平台的bk biz id

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public int getBkBizId() {
        return bkBizId;
    }

    public void setBkBizId(int bkBizId) {
        this.bkBizId = bkBizId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
