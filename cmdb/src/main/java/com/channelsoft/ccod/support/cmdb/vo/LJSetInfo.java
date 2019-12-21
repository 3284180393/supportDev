package com.channelsoft.ccod.support.cmdb.vo;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * @ClassName: LJSetInfo
 * @Author: lanhb
 * @Description: 用来定义从蓝鲸paas获取的set信息
 * @Date: 2019/12/2 11:36
 * @Version: 1.0
 */
public class LJSetInfo {

    private int bkBizId; //set对应的biz的id

    private int bkSetId; //set id

    private String bkSetName; //set名

    public int getBkBizId() {
        return bkBizId;
    }

    @JSONField(name = "bk_biz_id")
    public void setBkBizId(int bkBizId) {
        this.bkBizId = bkBizId;
    }

    public int getBkSetId() {
        return bkSetId;
    }

    @JSONField(name = "bk_set_id")
    public void setBkSetId(int bkSetId) {
        this.bkSetId = bkSetId;
    }

    public String getBkSetName() {
        return bkSetName;
    }

    @JSONField(name = "bk_set_name")
    public void setBkSetName(String bkSetName) {
        this.bkSetName = bkSetName;
    }
}
