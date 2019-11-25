package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.vo.DeployFileInfo;
import com.gitee.sunchenbin.mybatis.actable.annotation.Column;
import com.gitee.sunchenbin.mybatis.actable.annotation.Table;
import com.gitee.sunchenbin.mybatis.actable.command.BaseModel;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlTypeConstant;

import java.util.Date;

/**
 * @ClassName: AppCfgFilePo
 * @Author: lanhb
 * @Description: 用来定义app配置文件的类
 * @Date: 2019/11/12 16:46
 * @Version: 1.0
 */
public class AppCfgFilePo extends BaseModel {

    private int cfgFileId; //配置文件id,数据库唯一生成

    private int appId; //该配置文件是哪个app的配置文件,外键app表的app_id

    private String fileName; //配置文件名

    private String fileType; //配置文件类型，例如zip,tar,war,binary等,由FileType枚举预定义

    private String deployPath; //文件存放路径,可以是相对app的base path的相对路径也可以是绝对路径

    private String nexusRepository; //保存在nexus的仓库名

    private String nexusDirectory; //在nexus的保存路径

    private String nexusAssetId; //在nexus中的assetId

    private Date createTime; //该文件在nexus的创建时间

    private String md5; //该配置文件的md5特征值

    public AppCfgFilePo()
    {}

    public AppCfgFilePo(int appId, DeployFileInfo cfgFileInfo)
    {
        this.appId = appId;
        this.nexusAssetId = cfgFileInfo.getNexusAssetId();
        this.fileName = cfgFileInfo.getFileName();
        this.fileType = cfgFileInfo.getExt();
        this.deployPath = cfgFileInfo.getDeployPath();
        this.nexusAssetId = cfgFileInfo.getNexusAssetId();
        this.nexusDirectory = cfgFileInfo.getNexusDirectory();
        this.nexusRepository = cfgFileInfo.getNexusRepository();
        this.createTime = new Date();
        this.md5 = cfgFileInfo.getFileMd5();
    }

    public int getCfgFileId() {
        return cfgFileId;
    }

    public void setCfgFileId(int cfgFileId) {
        this.cfgFileId = cfgFileId;
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getNexusDirectory() {
        return nexusDirectory;
    }

    public void setNexusDirectory(String nexusDirectory) {
        this.nexusDirectory = nexusDirectory;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
