package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.*;
import com.channelsoft.ccod.support.cmdb.po.PlatformBase;
import com.channelsoft.ccod.support.cmdb.po.PlatformPo;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @ClassName: PlatformCreateParamVo
 * @Author: lanhb
 * @Description: 平台创建参数
 * @Date: 2020/2/18 22:02
 * @Version: 1.0
 */
public class PlatformCreateParamVo extends PlatformBase implements Serializable {

    @NotNull(message = "schemaId can not be null")
    private String schemaId; //平台升级计划id，用来唯一标识该计划的id

    private String params; //同平台创建相关的参数

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getSchemaId() {
        return schemaId;
    }

    public void setSchemaId(String schemaId) {
        this.schemaId = schemaId;
    }

    public PlatformPo getCreatePlatform()
    {
        String desc;
        switch (createMethod)
        {
            case CLONE:
                desc = String.format("create by %s %s", createMethod.name, params);
                break;
            default:
                desc = String.format("create by %s", createMethod.name);
        }
        PlatformPo po = new PlatformPo(this, CCODPlatformStatus.SCHEMA_CREATE, desc);
        return po;
    }

    public PlatformUpdateSchemaInfo getPlatformCreateSchema(List<DomainUpdatePlanInfo> domainPlanList)
    {
        String title = String.format("%s[%s]新建计划", platformName, platformId);
        String comment = PlatformCreateMethod.CLONE.equals(createMethod) ? String.format("create by %s %s", createMethod.name, params) : String.format("create by %s", createMethod.name);
        PlatformUpdateSchemaInfo schema = new PlatformUpdateSchemaInfo(this, PlatformUpdateTaskType.CREATE, UpdateStatus.CREATE, title, comment);
        schema.setDomainUpdatePlanList(domainPlanList);
        return schema;
    }
}
