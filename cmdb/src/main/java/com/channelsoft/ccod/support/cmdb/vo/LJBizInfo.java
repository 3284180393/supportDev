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
    private int bkBizId;

    private String bkBizName;

    public int getBkBizId() {
        return bkBizId;
    }

    @JSONField(name = "bk_biz_id")
    public void setBkBizId(int bkBizId) {
        this.bkBizId = bkBizId;
    }

    public String getBkBizName() {
        return bkBizName;
    }

    @JSONField(name = "bk_biz_name")
    public void setBkBizName(String bkBizName) {
        this.bkBizName = bkBizName;
    }

    @Override
    public String toString()
    {
        return String.format("bkBizId=%d and bkBizName=%s", bkBizId, bkBizName);
    }
}
