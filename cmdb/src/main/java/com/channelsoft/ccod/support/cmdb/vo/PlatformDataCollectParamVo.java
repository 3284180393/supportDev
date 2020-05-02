package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.PlatformDataCollectContent;

/**
 * @ClassName: PlatformDataCollectParamVo
 * @Author: lanhb
 * @Description: 用来定义平台收集相关参数
 * @Date: 2020/3/13 11:41
 * @Version: 1.0
 */
public class PlatformDataCollectParamVo {

    private PlatformDataCollectContent collectContent; //平台收集内容,不能为空

    private String platformId; //平台id,不能为空

    private String platformName; //平台名,不能为空

    private String domainName; //域名可以为空

    private String hostIp; //主机ip可以为空

    private String appName; //应用名，可以为空

    private String version; //应用版本，可以为空

    private int bkBizId; //平台所属的蓝鲸paas的biz id

    private int bkCloudId; //平台服务器所属的蓝鲸paas的cloud id

    public PlatformDataCollectContent getCollectContent() {
        return collectContent;
    }

    public void setCollectContent(PlatformDataCollectContent collectContent) {
        this.collectContent = collectContent;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getBkBizId() {
        return bkBizId;
    }

    public void setBkBizId(int bkBizId) {
        this.bkBizId = bkBizId;
    }

    public int getBkCloudId() {
        return bkCloudId;
    }

    public void setBkCloudId(int bkCloudId) {
        this.bkCloudId = bkCloudId;
    }
}
