package com.channelsoft.ccod.support.cmdb.vo;

/**
 * @ClassName: LJSetInfo
 * @Author: lanhb
 * @Description: 用来定义从蓝鲸paas获取的set信息
 * @Date: 2019/12/2 11:36
 * @Version: 1.0
 */
public class LJSetInfo {
    private int setId; //set id

    private String setName; //set名

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
}
