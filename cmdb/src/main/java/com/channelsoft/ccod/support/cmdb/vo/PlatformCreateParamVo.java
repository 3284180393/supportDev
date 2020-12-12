package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.*;
import com.channelsoft.ccod.support.cmdb.po.PlatformBase;
import com.channelsoft.ccod.support.cmdb.po.PlatformPo;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
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

    private ServiceAccessMethod threePartAppServiceAccessMethod; //用来定义第三方服务的访问方式

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

    public ServiceAccessMethod getThreePartAppServiceAccessMethod() {
        return threePartAppServiceAccessMethod;
    }

    public void setThreePartAppServiceAccessMethod(ServiceAccessMethod threePartAppServiceAccessMethod) {
        this.threePartAppServiceAccessMethod = threePartAppServiceAccessMethod;
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
        Map<String, Object> params = new HashMap<>();
        return getPlatformCreateSchema(this.cfgs, params, domainPlanList);
    }

    public PlatformUpdateSchemaInfo getPlatformCreateSchema(List<AppFileNexusInfo> cfgs, Map<String, Object> params, List<DomainUpdatePlanInfo> domainPlanList)
    {
        this.cfgs = cfgs;
        String title = String.format("%s[%s]新建计划", platformName, platformId);
        String comment = PlatformCreateMethod.CLONE.equals(createMethod) ? String.format("create by %s %s", createMethod.name, params) : String.format("create by %s", createMethod.name);
        PlatformUpdateSchemaInfo schema = new PlatformUpdateSchemaInfo(this, params, PlatformUpdateTaskType.CREATE, UpdateStatus.CREATE, title, comment);
        schema.setSchemaId(schemaId);
        schema.setDomains(domainPlanList);
        return schema;
    }
}
