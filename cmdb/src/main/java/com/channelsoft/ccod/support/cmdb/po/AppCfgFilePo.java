package com.channelsoft.ccod.support.cmdb.po;

import java.util.Date;

/**
 * @ClassName: AppCfgFilePo
 * @Author: lanhb
 * @Description: 用来定义app配置文件的类
 * @Date: 2019/11/12 16:46
 * @Version: 1.0
 */
public class AppCfgFilePo {
    private int cfgFileId; //配置文件id,数据库唯一生成

    private int appId; //该配置文件是哪个app的配置文件

    private String fileName; //配置文件名

    private String savePath; //文件存放路径,可以是相对app的base path的相对路径,也可以是绝对路径

    private String nexusName; //该配置文件在nexus中的name

    private Date uploadTime; //该文件上传到nexus的时间

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

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public String getNexusName() {
        return nexusName;
    }

    public void setNexusName(String nexusName) {
        this.nexusName = nexusName;
    }

    public Date getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(Date uploadTime) {
        this.uploadTime = uploadTime;
    }
}
