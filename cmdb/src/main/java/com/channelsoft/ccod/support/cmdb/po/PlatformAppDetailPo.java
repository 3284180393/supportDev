package com.channelsoft.ccod.support.cmdb.po;

/**
 * @ClassName: PlatformAppDetailPo
 * @Author: lanhb
 * @Description: 用来定义平台应用详情, 这里的详情可以是任意信息, 用键值对表示
 * @Date: 2019/11/12 17:40
 * @Version: 1.0
 */
public class PlatformAppDetailPo {
    private int id; //数据库唯一主键

    private String platformId; //平台id

    private String platformName; //平台名

    private String domainId; //域id

    private String domainName; //域名

    private String hostIp; //主机ip

    private String hostname; //主机名

    private String appType; //app类型

    private String appName; //app名

    private String appAlias; //app别名

    private String key; //应用详情中的key

    private String value; //应用详情中的value

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

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppAlias() {
        return appAlias;
    }

    public void setAppAlias(String appAlias) {
        this.appAlias = appAlias;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
