package com.channelsoft.ccod.support.cmdb.po;

/**
 * @ClassName: PlatformAppBkModulePo
 * @Author: lanhb
 * @Description: 平台应用在蓝鲸paas上的所有关联信息
 * @Date: 2019/12/19 16:00
 * @Version: 1.0
 */
public class PlatformAppBkModulePo {

    private int appBkModuleId; //主键数据库唯一生成

    private int platformAppId; //平台应用id,外键platform_app表

    private String platformId; //部署应用的平台id

    private String domainId; //部署应用的域id

    private String setId; //平台应用归属ccod的set id

    private int bkBizId; //该应用在蓝鲸paas的biz的id

    private int bkSetId; //该应用在蓝鲸set的id

    private String bkSetName; //该应用在所在蓝鲸paas的set名

    private int bkModuleId; //该应用对应蓝鲸的module的id

    private int bkHostId; //该应用在蓝鲸的host的id

    public int getAppBkModuleId() {
        return appBkModuleId;
    }

    public void setAppBkModuleId(int appBkModuleId) {
        this.appBkModuleId = appBkModuleId;
    }

    public int getPlatformAppId() {
        return platformAppId;
    }

    public void setPlatformAppId(int platformAppId) {
        this.platformAppId = platformAppId;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getSetId() {
        return setId;
    }

    public void setSetId(String setId) {
        this.setId = setId;
    }

    public int getBkBizId() {
        return bkBizId;
    }

    public void setBkBizId(int bkBizId) {
        this.bkBizId = bkBizId;
    }

    public int getBkSetId() {
        return bkSetId;
    }

    public void setBkSetId(int bkSetId) {
        this.bkSetId = bkSetId;
    }

    public String getBkSetName() {
        return bkSetName;
    }

    public void setBkSetName(String bkSetName) {
        this.bkSetName = bkSetName;
    }

    public int getBkHostId() {
        return bkHostId;
    }

    public void setBkHostId(int bkHostId) {
        this.bkHostId = bkHostId;
    }

    public int getBkModuleId() {
        return bkModuleId;
    }

    public void setBkModuleId(int bkModuleId) {
        this.bkModuleId = bkModuleId;
    }
}
