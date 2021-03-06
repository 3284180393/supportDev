package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.vo.DeployFileInfo;

import java.util.Date;

/**
 * @ClassName: AppFilePo
 * @Author: lanhb
 * @Description: 应用文件纯虚基类
 * @Date: 2020/1/7 10:18
 * @Version: 1.0
 */
public class AppFilePo {

    protected int appId; //该配置文件是哪个app的关联文件,外键app表的app_id

    protected String fileName; //文件名

    protected String ext; //文件类型，例如zip,tar,war,binary等,由FileType枚举预定义

    protected String deployPath; //文件存放路径,可以是相对app的base path的相对路径也可以是绝对路径

    protected String nexusRepository; //文件保存在nexus的仓库名

    protected String nexusDirectory; //文件保存在nexus的路径

    protected String nexusAssetId; //文件保存在nexus中的assetId

    protected Date createTime; //该文件在nexus的创建时间

    protected String md5; //该配置文件的md5特征值

    public AppFilePo(int appId, DeployFileInfo cfgFileInfo)
    {
        this.appId = appId;
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

    public AppFilePo()
    {

    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
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

    public String getFileNexusDownloadUrl(String nexusHostUrl)
    {
        String downloadUrl = String.format("%s/repository/%s/%s/%s", nexusHostUrl, this.nexusRepository, this.nexusDirectory, this.fileName);
        return downloadUrl;
    }

    public String getNexusFileSavePath()
    {
        String path = String.format("/%s/%s", this.nexusDirectory, this.getFileName());
        return path;
    }
}
