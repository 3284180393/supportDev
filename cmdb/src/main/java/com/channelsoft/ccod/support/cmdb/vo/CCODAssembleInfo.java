package com.channelsoft.ccod.support.cmdb.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: CCODAssembleInfo
 * @Author: lanhb
 * @Description: 用来在拓扑关联到assemble
 * @Date: 2020/6/4 14:53
 * @Version: 1.0
 */
public class CCODAssembleInfo {

    private String assembleTag; //对应的assemble tag

    private List<CCODModuleInfo> modules;

    public CCODAssembleInfo(String assembleTag)
    {
        this.assembleTag = assembleTag;
        this.modules = new ArrayList<>();
    }

    public String getAssembleTag() {
        return assembleTag;
    }

    public void setAssembleTag(String assembleTag) {
        this.assembleTag = assembleTag;
    }

    public List<CCODModuleInfo> getModules() {
        return modules;
    }

    public void setModules(List<CCODModuleInfo> modules) {
        this.modules = modules;
    }

    @Override
    public CCODAssembleInfo clone()
    {
        CCODAssembleInfo assembleInfo = new CCODAssembleInfo(this.assembleTag);
        List<CCODModuleInfo> moduleList = new ArrayList<>();
        for(CCODModuleInfo module : this.modules)
        {
            moduleList.add(module.clone());
        }
        assembleInfo.setModules(moduleList);
        return assembleInfo;
    }
}
