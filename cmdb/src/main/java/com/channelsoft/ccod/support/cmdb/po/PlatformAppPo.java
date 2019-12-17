package com.channelsoft.ccod.support.cmdb.po;

import java.util.Date;

/**
 * @ClassName: PlatformAppPo
 * @Author: lanhb
 * @Description: 用来定义平台应用部署情况的类
 * @Date: 2019/11/12 17:04
 * @Version: 1.0
 */
public class PlatformAppPo {

    private int platformAppId; //平台app部署id,数据库唯一生成

    private int appId; //应用id,外键app的appId

    private String appAlias; //应用别名,例如在服务器上部署两个cms,appName=cmsserver,两个cms的别名可以分别取cms1和cms2用来区分

    private String platformId; //平台id

    private String setId; //该应用所在ccod应用平台的set id

    private String domainId; //应用所在的域id,外键domain的domain_id

    private int serverId; //应用部署所在服务器id,外键server表的server_id

    private int runnerId; //应用运行用户id

    private String hostIp; //应用所在服务器的主机ip

    private String appRunner; //应用运行用户

    private String basePath; //该应用的basePath

    private Date deployTime; //该应用的部署路径

    private int bkBizId; //该应用在蓝鲸paas的biz的id

    private int bkSetId; //该应用在蓝鲸set的id

    private String bkSetName; //该应用在所在蓝鲸paas的set名

    private int bkHostId; //该应用在蓝鲸的host的id

    private int bkModuleId; //该应用对应蓝鲸的module的id

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

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public Date getDeployTime() {
        return deployTime;
    }

    public void setDeployTime(Date deployTime) {
        this.deployTime = deployTime;
    }

    public int getRunnerId() {
        return runnerId;
    }

    public void setRunnerId(int runnerId) {
        this.runnerId = runnerId;
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

    public String getAppAlias() {
        return appAlias;
    }

    public void setAppAlias(String appAlias) {
        this.appAlias = appAlias;
    }

    public String getSetId() {
        return setId;
    }

    public void setSetId(String setId) {
        this.setId = setId;
    }

    public String getBkSetName() {
        return bkSetName;
    }

    public void setBkSetName(String bkSetName) {
        this.bkSetName = bkSetName;
    }

    public String getAppRunner() {
        return appRunner;
    }

    public void setAppRunner(String appRunner) {
        this.appRunner = appRunner;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }
}
