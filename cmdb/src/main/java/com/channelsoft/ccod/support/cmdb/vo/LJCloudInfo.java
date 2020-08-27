package com.channelsoft.ccod.support.cmdb.vo;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * @ClassName: LJCloudInfo
 * @Author: lanhb
 * @Description: 用来定义蓝鲸云（机房）信息
 * @Date: 2020/8/27 14:11
 * @Version: 1.0
 */
public class LJCloudInfo {

    private int id;

    private String name;

    public int getId() {
        return id;
    }

    @JSONField(name = "id")
    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    @JSONField(name = "bk_inst_name")
    public void setName(String name) {
        this.name = name;
    }
}
