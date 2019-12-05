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

    private int bizId; //set对应的biz的id

    private int setId; //set id

    private String setName; //set名

    @JSONField(name = "bk_biz_id")
    public void setBizId(int bizId) {
        this.bizId = bizId;
    }

    public int getSetId() {
        return setId;
    }

    @JSONField(name = "bk_set_id")
    public void setSetId(int setId) {
        this.setId = setId;
    }

    public String getSetName() {
        return setName;
    }

    @JSONField(name = "bk_set_name")
    public void setSetName(String setName) {
        this.setName = setName;
    }

    public int getBizId() {
        return bizId;
    }
}
