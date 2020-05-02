package com.channelsoft.ccod.support.cmdb.po;

import java.util.Date;

/**
 * @ClassName: UnconfirmedAppModulePo
 * @Author: lanhb
 * @Description: 用来定义onlinemanager收集的但收集的信息有误应用模块信息的pojo类
 * @Date: 2020/4/28 15:37
 * @Version: 1.0
 */
public class UnconfirmedAppModulePo {

    private int id; //id,数据库自动生成主键

    private String platformId; //平台id

    private String domainName; //域名

    private String hostIp; //服务器ip

    private String appName; //应用名

    private String alias; //应用别名

    private String reason; //为能确认原因

    private Date submitTime; //提交时间

    private String packageDownloadUrl; //程序包下载路径

    private String cfgDownloadUrl; //配置文件下载地址;多个配置文件用;分割


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
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

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Date getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime) {
        this.submitTime = submitTime;
    }

    public String getPackageDownloadUrl() {
        return packageDownloadUrl;
    }

    public void setPackageDownloadUrl(String packageDownloadUrl) {
        this.packageDownloadUrl = packageDownloadUrl;
    }

    public String getCfgDownloadUrl() {
        return cfgDownloadUrl;
    }

    public void setCfgDownloadUrl(String cfgDownloadUrl) {
        this.cfgDownloadUrl = cfgDownloadUrl;
    }

    @Override
    public String toString()
    {
        String str = String.format("%s(%s) at %s of %s in %s is unconfirmed [%s]",
                alias, appName, hostIp, domainName, platformId, reason);
        return str;
    }
}
