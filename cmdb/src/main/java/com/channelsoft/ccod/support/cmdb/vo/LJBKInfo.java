package com.channelsoft.ccod.support.cmdb.vo;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * @ClassName: LJBKInfo
 * @Author: lanhb
 * @Description: 用来定义从蓝鲸paas获取的bk信息
 * @Date: 2019/12/2 11:11
 * @Version: 1.0
 */
public class LJBKInfo {
    private int bizId;

    private String bizName;

    public int getBizId() {
        return bizId;
    }

    @JSONField(name = "bk_biz_id")
    public void setBizId(int bizId) {
        this.bizId = bizId;
    }

    public String getBizName() {
        return bizName;
    }

    @JSONField(name = "bk_biz_name")
    public void setBizName(String bizName) {
        this.bizName = bizName;
    }
}
