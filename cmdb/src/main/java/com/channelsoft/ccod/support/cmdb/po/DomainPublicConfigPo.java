package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.vo.AppFileNexusInfo;

/**
 * @ClassName: DomainPublicConfigPo
 * @Author: lanhb
 * @Description: 用来定义域公共配置的pojo类
 * @Date: 2020/6/22 18:13
 * @Version: 1.0
 */
public class DomainPublicConfigPo extends AppFileNexusInfo {

    private int domPubCfgId; //域公共配置id，主键

    private String platformId; //平台id

    private String domainId; //域id

    public DomainPublicConfigPo()
    {}

    public DomainPublicConfigPo(String domainId, String platformId, String deployPath, NexusAssetInfo assetInfo)
    {
        super(assetInfo, deployPath);
        this.domainId = domainId;
        this.platformId = platformId;
    }

    public int getDomPubCfgId() {
        return domPubCfgId;
    }

    public void setDomPubCfgId(int domPubCfgId) {
        this.domPubCfgId = domPubCfgId;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }
}
