package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.vo.AppFileNexusInfo;
import com.channelsoft.ccod.support.cmdb.vo.DeployFileInfo;

import java.util.Date;

/**
 * @ClassName: PlatformAppCfgFilePo
 * @Author: lanhb
 * @Description: 用来定义平台配置文件信息的类
 * @Date: 2019/11/20 10:49
 * @Version: 1.0
 */
public class PlatformAppCfgFilePo extends AppFilePo {

    private int cfgFileId; //配置文件id,数据库唯一生成

    private int platformAppId; //平台app id,引用 platform_app表id为外键

    public PlatformAppCfgFilePo()
    {}

    public PlatformAppCfgFilePo(int platformAppId, DeployFileInfo cfgFileInfo)
    {
        this.platformAppId = platformAppId;
        this.nexusAssetId = cfgFileInfo.getNexusAssetId();
        this.fileName = cfgFileInfo.getFileName();
        this.ext = cfgFileInfo.getExt();
        this.deployPath = cfgFileInfo.getDeployPath();
        this.nexusAssetId = cfgFileInfo.getNexusAssetId();
        this.nexusDirectory = cfgFileInfo.getNexusDirectory();
        this.nexusRepository = cfgFileInfo.getNexusRepository();
        this.createTime = new Date();
        this.md5 = cfgFileInfo.getFileMd5();
    }

    public PlatformAppCfgFilePo(int platformAppId, int appId, String deployPath, NexusAssetInfo assetInfo)
    {
        super(appId, deployPath, assetInfo);
        this.platformAppId = platformAppId;
    }

    public PlatformAppCfgFilePo(int appId, String deployPath, NexusAssetInfo assetInfo)
    {
        super(appId, deployPath, assetInfo);
        this.platformAppId = 0;
    }

    public PlatformAppCfgFilePo(int appId, AppFileNexusInfo fileInfo, String nexusHostUrl)
    {
        super(appId, fileInfo.getDeployPath(), fileInfo.getNexusAssetInfo(nexusHostUrl));
        this.platformAppId = 0;
    }

    public int getCfgFileId() {
        return cfgFileId;
    }

    public void setCfgFileId(int cfgFileId) {
        this.cfgFileId = cfgFileId;
    }

    public int getPlatformAppId() {
        return platformAppId;
    }

    public void setPlatformAppId(int platformAppId) {
        this.platformAppId = platformAppId;
    }

    public String getFileNexusSavePath()
    {
        String path = String.format("%s/%s", this.getNexusDirectory(), this.fileName);
        return path;
    }
}
