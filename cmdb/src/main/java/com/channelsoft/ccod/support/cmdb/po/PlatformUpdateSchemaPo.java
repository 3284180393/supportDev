package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.constant.PlatformUpdateTaskType;
import com.channelsoft.ccod.support.cmdb.constant.UpdateStatus;

import java.util.Date;

/**
 * @ClassName: PlatformUpdateSchemaPo
 * @Author: lanhb
 * @Description: 用来定义平台升级计划的pojo类
 * @Date: 2019/12/11 9:13
 * @Version: 1.0
 */
public class PlatformUpdateSchemaPo {

    private int schemaId; //平台升级计划id,数据库主键

    private String platformId; //该计划对应的平台

    private String context; //计划类容

    public int getSchemaId() {
        return schemaId;
    }

    public void setSchemaId(int schemaId) {
        this.schemaId = schemaId;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
