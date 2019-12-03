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
    private int bizId;

    private int setId;

    private int moduleId;

    private String moduleName;

    private String moduleType;

    public int getBizId() {
        return bizId;
    }

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

    public int getModuleId() {
        return moduleId;
    }

    @JSONField(name = "bk_module_id")
    public void setModuleId(int moduleId) {
        this.moduleId = moduleId;
    }

    public String getModuleName() {
        return moduleName;
    }

    @JSONField(name = "bk_module_name")
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getModuleType() {
        return moduleType;
    }

    @JSONField(name = "bk_module_type")
    public void setModuleType(String moduleType) {
        this.moduleType = moduleType;
    }

}
