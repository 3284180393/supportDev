package com.channelsoft.ccod.support.cmdb.po;

import com.gitee.sunchenbin.mybatis.actable.annotation.Column;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlTypeConstant;

import java.util.Date;

/**
 * @ClassName: AppPackagePo
 * @Author: lanhb
 * @Description: 用来定义应用发布包的类
 * @Date: 2019/11/12 16:56
 * @Version: 1.0
 */
public class AppPackagePo {
    private int packageId; //发布包id,数据库唯一生成主键

    private int appId; //该发布包对应的是哪个应用的id,外键app表的appId

    private String fileName; //安装包文件名

    private String deployPath; //文件存放路径,可以是相对app的base path的相对路径,

    private String nexusRepository; //保存在nexus的仓库名

    private String nexusAssetId; //在nexus中的assetId

    private String nexusDownloadUrl; //在nexus中的download url

    private Date createTime; //该文件在nexus的创建时间

    private String md5; //该安装包的md5特征值

    private String sourceFilePath; //上传源文件路径,不需要入库

    public int getPackageId() {
        return packageId;
    }

    public void setPackageId(int packageId) {
        this.packageId = packageId;
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

    public String getNexusAssetId() {
        return nexusAssetId;
    }

    public void setNexusAssetId(String nexusAssetId) {
        this.nexusAssetId = nexusAssetId;
    }

    public String getNexusDownloadUrl() {
        return nexusDownloadUrl;
    }

    public void setNexusDownloadUrl(String nexusDownloadUrl) {
        this.nexusDownloadUrl = nexusDownloadUrl;
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

    public String getSourceFilePath() {
        return sourceFilePath;
    }

    public void setSourceFilePath(String sourceFilePath) {
        this.sourceFilePath = sourceFilePath;
    }
}
