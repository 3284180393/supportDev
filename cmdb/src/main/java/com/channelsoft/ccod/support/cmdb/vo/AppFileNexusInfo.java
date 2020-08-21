package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.po.Checksum;
import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;

/**
 * @ClassName: AppFileNexusInfo
 * @Author: lanhb
 * @Description: 用来定义应用文件(安装包 、 配置文件)的nexus存储信息
 * @Date: 2019/12/23 14:46
 * @Version: 1.0
 */
public class AppFileNexusInfo {

    protected String fileName;  //文件名

    protected String ext; //文件扩展

    protected String md5; //文件md5

    protected String deployPath; //文件部署路径

    protected String nexusRepository; //存放文件的nexus仓库

    protected String nexusPath; //该文件在nexus的存放path

    protected String nexusAssetId; //该文件的assetId

    public AppFileNexusInfo()
    {}

    public AppFileNexusInfo(NexusAssetInfo assetInfo, String deployPath)
    {
        this.fileName = assetInfo.getNexusAssetFileName();
        String[] arr = fileName.split("\\-");
        this.ext = arr.length > 1 ? arr[arr.length-1] : "";
        this.md5 = assetInfo.getMd5();
        this.nexusRepository = assetInfo.getRepository();
        this.nexusPath = assetInfo.getPath();
        this.nexusAssetId = assetInfo.getId();
        this.deployPath = deployPath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getDeployPath() {
        return deployPath;
    }

    public void setDeployPath(String deployPath) {
        this.deployPath = deployPath;
    }

    public String getNexusRepository() {
        return nexusRepository;
    }

    public void setNexusRepository(String nexusRepository) {
        this.nexusRepository = nexusRepository;
    }

    public String getNexusPath() {
        return nexusPath;
    }

    public void setNexusPath(String nexusPath) {
        this.nexusPath = nexusPath;
    }

    public String getNexusAssetId() {
        return nexusAssetId;
    }

    public void setNexusAssetId(String nexusAssetId) {
        this.nexusAssetId = nexusAssetId;
    }

    public String getFileNexusDownloadUrl(String nexusHostUrl)
    {
        String downloadUrl = String.format("%s/repository/%s/%s", nexusHostUrl, this.nexusRepository, this.nexusPath);
        return downloadUrl;
    }

    public NexusAssetInfo getNexusAssetInfo(String nexusHostUrl)
    {
        NexusAssetInfo assetInfo = new NexusAssetInfo();
        assetInfo.setRepository(this.nexusRepository);
        assetInfo.setDownloadUrl(this.getFileNexusDownloadUrl(nexusHostUrl));
        assetInfo.setFormat("raw");
        Checksum checksum = new Checksum();
        checksum.md5 = this.md5;
        assetInfo.setChecksum(checksum);
        assetInfo.setId(this.nexusAssetId);
        assetInfo.setPath(this.nexusPath);
        return assetInfo;
    }

    public AppFileNexusInfo getAppFileNexusInfo()
    {
        AppFileNexusInfo fileInfo = new AppFileNexusInfo();
        fileInfo.fileName = fileName;
        fileInfo.ext = ext;
        fileInfo.md5 = md5;
        fileInfo.deployPath = deployPath;
        fileInfo.nexusRepository = nexusRepository;
        fileInfo.nexusPath = nexusPath;
        fileInfo.nexusAssetId = nexusAssetId;
        return fileInfo;
    }
}
