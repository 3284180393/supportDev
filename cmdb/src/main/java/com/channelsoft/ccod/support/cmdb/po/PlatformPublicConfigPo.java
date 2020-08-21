package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.vo.AppFileNexusInfo;
import com.channelsoft.ccod.support.cmdb.vo.DeployFileInfo;

/**
 * @ClassName: PlatformPublicConfigPo
 * @Author: lanhb
 * @Description: 用来定义平台公共配置的pojo类
 * @Date: 2020/6/22 17:49
 * @Version: 1.0
 */
public class PlatformPublicConfigPo extends AppFileNexusInfo {

    private int platPubCfgId; //平台公共配置id，主键

    private String platformId; //平台id

    public PlatformPublicConfigPo()
    {}

    public PlatformPublicConfigPo(String platformId, AppFileNexusInfo fileInfo)
    {
        this.platformId = platformId;
        this.nexusPath = fileInfo.getNexusPath();
        this.nexusRepository = fileInfo.getNexusRepository();
        this.fileName = fileInfo.getFileName();
        this.md5 = fileInfo.getMd5();
        this.deployPath = fileInfo.getDeployPath();
        this.nexusAssetId = fileInfo.getNexusAssetId();
        String[] arr = this.fileName.split("\\.");
        this.ext = arr.length == 1 ? null : arr[arr.length - 1];
    }

    public PlatformPublicConfigPo(String platformId, String deployPath, NexusAssetInfo assetInfo)
    {
        super(assetInfo, deployPath);
        this.platformId = platformId;
    }

    public int getPlatPubCfgId() {
        return platPubCfgId;
    }

    public void setPlatPubCfgId(int platPubCfgId) {
        this.platPubCfgId = platPubCfgId;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }
}
