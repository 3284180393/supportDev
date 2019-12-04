package com.channelsoft.ccod.support.cmdb.vo;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * @ClassName: LJBizInfo
 * @Author: lanhb
 * @Description: 用来定义从蓝鲸paas获取的biz信息
 * @Date: 2019/12/4 13:50
 * @Version: 1.0
 */
public class LJBizInfo {
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
