package com.channelsoft.ccod.support.cmdb.vo;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * @ClassName: LJModuleInfo
 * @Author: lanhb
 * @Description: 用来定义蓝鲸paas平台的服务器部署模块信息
 * @Date: 2019/12/2 15:41
 * @Version: 1.0
 */
public class LJModuleInfo {

    private int bkBizId;

    private int bkSetId;

    private int bkModuleId;

    private String bkModuleName;

    private String bkModuleType;

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

    public int getBkModuleId() {
        return bkModuleId;
    }

    @JSONField(name = "bk_module_id")
    public void setBkModuleId(int bkModuleId) {
        this.bkModuleId = bkModuleId;
    }

    public String getBkModuleName() {
        return bkModuleName;
    }

    @JSONField(name = "bk_module_name")
    public void setBkModuleName(String bkModuleName) {
        this.bkModuleName = bkModuleName;
    }

    public String getBkModuleType() {
        return bkModuleType;
    }

    @JSONField(name = "bk_module_type")
    public void setBkModuleType(String bkModuleType) {
        this.bkModuleType = bkModuleType;
    }
}
