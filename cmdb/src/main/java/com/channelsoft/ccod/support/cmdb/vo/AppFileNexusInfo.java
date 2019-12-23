package com.channelsoft.ccod.support.cmdb.vo;

/**
 * @ClassName: AppFileNexusInfo
 * @Author: lanhb
 * @Description: 用来定义应用文件(安装包 、 配置文件)的nexus存储信息
 * @Date: 2019/12/23 14:46
 * @Version: 1.0
 */
public class AppFileNexusInfo {

    private String fileName;  //文件名

    private String ext; //文件扩展

    private long fileSize; //文件大小

    private String md5; //文件md5

    private String deployPath; //文件部署路径

    private String nexusRepository; //存放文件的nexus仓库

    private String nexusPath; //该文件在nexus的存放path

    private String nexusAssetId; //该文件的assetId

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

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
