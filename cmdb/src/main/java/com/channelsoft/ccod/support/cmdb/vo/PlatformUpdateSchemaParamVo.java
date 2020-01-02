package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.PlatformUpdateTaskType;

import java.util.List;

/**
 * @ClassName: PlatformUpdateSchemaParamVo
 * @Author: lanhb
 * @Description: 用来定义一个创建平台升级需要的相关参数
 * @Date: 2019/12/25 11:10
 * @Version: 1.0
 */
public class PlatformUpdateSchemaParamVo {

    private PlatformUpdateTaskType taskType; //平台升级计划类型

    private String platformId; //平台id

    private String platformName; //平台名

    private int bkBizId; //平台对应蓝鲸paas的biz id

    private int bkCloudId; //平台服务器所在云的id

    private List<String> planAppList; //计划的应用列表

    public PlatformUpdateTaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(PlatformUpdateTaskType taskType) {
        this.taskType = taskType;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public int getBkBizId() {
        return bkBizId;
    }

    public void setBkBizId(int bkBizId) {
        this.bkBizId = bkBizId;
    }

    public int getBkCloudId() {
        return bkCloudId;
    }

    public void setBkCloudId(int bkCloudId) {
        this.bkCloudId = bkCloudId;
    }

    public List<String> getPlanAppList() {
        return planAppList;
    }

    public void setPlanAppList(List<String> planAppList) {
        this.planAppList = planAppList;
    }
}
