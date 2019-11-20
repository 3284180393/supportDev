package com.channelsoft.ccod.support.cmdb.po;

import java.util.Date;

/**
 * @ClassName: PlatformAppCfgFilePo
 * @Author: lanhb
 * @Description: 用来定义平台配置文件信息的类
 * @Date: 2019/11/20 10:49
 * @Version: 1.0
 */
public class PlatformAppCfgFilePo {

    private int cfgFileId; //配置文件id,数据库唯一生成

    private int platformAppId; //平台app id,引用 platform_app表id为外键

    private String fileName; //配置文件名

    private String nexusRepository; //保存在nexus的仓库名

    private String nexusDirectory; //保存在nexus的路径

    private String nexusAssetId; //在nexus中的assetId

    private Date createTime; //该文件在nexus的创建时间

    private String md5; //该配置文件的md5特征值

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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getNexusRepository() {
        return nexusRepository;
    }

    public void setNexusRepository(String nexusRepository) {
        this.nexusRepository = nexusRepository;
    }

    public String getNexusDirectory() {
        return nexusDirectory;
    }

    public void setNexusDirectory(String nexusDirectory) {
        this.nexusDirectory = nexusDirectory;
    }

    public String getNexusAssetId() {
        return nexusAssetId;
    }

    public void setNexusAssetId(String nexusAssetId) {
        this.nexusAssetId = nexusAssetId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }
}
