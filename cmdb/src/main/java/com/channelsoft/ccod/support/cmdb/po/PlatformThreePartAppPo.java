package com.channelsoft.ccod.support.cmdb.po;

/**
 * @ClassName: PlatformThreePartAppPo
 * @Author: lanhb
 * @Description: 用来定义平台第三方服务
 * @Date: 2020/6/3 16:19
 * @Version: 1.0
 */
public class PlatformThreePartAppPo {

    private int threePartAppId; //id，数据库主键

    private String platformId; //归属平台id

    private String appName;

    private String alias;

    private String version;

    private int replicas; //运行副本数目

    private int availableReplicas; //可用副本数目

    private String hostIp;

    private String port;

    private String status;

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public int getThreePartAppId() {
        return threePartAppId;
    }

    public void setThreePartAppId(int threePartAppId) {
        this.threePartAppId = threePartAppId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }

    public int getAvailableReplicas() {
        return availableReplicas;
    }

    public void setAvailableReplicas(int availableReplicas) {
        this.availableReplicas = availableReplicas;
    }


}
