package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.PlatformType;

import javax.validation.constraints.NotNull;

/**
 * @ClassName: PlatformSchemaParamVo
 * @Author: lanhb
 * @Description: 用来定义平台升级规律参数类
 * @Date: 2020/7/14 16:52
 * @Version: 1.0
 */
public class PlatformSchemaParamVo {

    @NotNull(message = "platformType can not be null")
    private PlatformType platformType;

    private PlatformUpdateSchemaInfo schemaInfo;

    public PlatformType getPlatformType() {
        return platformType;
    }

    public void setPlatformType(PlatformType platformType) {
        this.platformType = platformType;
    }

    public PlatformUpdateSchemaInfo getSchemaInfo() {
        return schemaInfo;
    }

    public void setSchemaInfo(PlatformUpdateSchemaInfo schemaInfo) {
        this.schemaInfo = schemaInfo;
    }

}
