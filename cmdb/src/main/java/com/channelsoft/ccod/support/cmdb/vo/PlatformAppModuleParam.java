package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.po.AssemblePo;
import com.channelsoft.ccod.support.cmdb.po.DomainPo;
import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;
import com.channelsoft.ccod.support.cmdb.po.PlatformAppPo;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: PlatformAppModuleParam
 * @Author: lanhb
 * @Description: 平台应用模块相关的参数
 * @Date: 2020/6/4 20:27
 * @Version: 1.0
 */
public class PlatformAppModuleParam {

    public PlatformAppModuleParam(AppModuleVo moduleVo, String alias, String platformId, DomainPo domainPo, AssemblePo assemblePo, PlatformAppPo platformAppPo, List<NexusAssetInfo> cfgs)
    {
        this.moduleVo = moduleVo;
        this.alias = alias;
        this.platformId = platformId;
        this.domainPo = domainPo;
        this.assemblePo = assemblePo;
        this.platformAppPo = platformAppPo;
        this.cfgs = cfgs;
    }

    private String alias;

    private String platformId;

    private AppModuleVo moduleVo;

    private DomainPo domainPo;

    private AssemblePo assemblePo;

    private PlatformAppPo platformAppPo;

    private List<NexusAssetInfo> cfgs;

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public AppModuleVo getModuleVo() {
        return moduleVo;
    }

    public void setModuleVo(AppModuleVo moduleVo) {
        this.moduleVo = moduleVo;
    }

    public DomainPo getDomainPo() {
        return domainPo;
    }

    public void setDomainPo(DomainPo domainPo) {
        this.domainPo = domainPo;
    }

    public AssemblePo getAssemblePo() {
        return assemblePo;
    }

    public void setAssemblePo(AssemblePo assemblePo) {
        this.assemblePo = assemblePo;
    }

    public PlatformAppPo getPlatformAppPo() {
        return platformAppPo;
    }

    public void setPlatformAppPo(PlatformAppPo platformAppPo) {
        this.platformAppPo = platformAppPo;
    }

    public List<NexusAssetInfo> getCfgs() {
        return cfgs;
    }

    public void setCfgs(List<NexusAssetInfo> cfgs) {
        this.cfgs = cfgs;
    }
}
