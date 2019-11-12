package com.channelsoft.ccod.support.cmdb.po;

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

    private String nexusName; //该发布包在nexus中的name

    private String md5; //该发布包的md5特征值

    private Date uploadTime; //该发布包的上传时间

    private String deployPath; //该发布包的部署路径,可以是绝对路径,也可是相对app的base path的相对路径

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

    public String getNexusName() {
        return nexusName;
    }

    public void setNexusName(String nexusName) {
        this.nexusName = nexusName;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public Date getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(Date uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getDeployPath() {
        return deployPath;
    }

    public void setDeployPath(String deployPath) {
        this.deployPath = deployPath;
    }
}
