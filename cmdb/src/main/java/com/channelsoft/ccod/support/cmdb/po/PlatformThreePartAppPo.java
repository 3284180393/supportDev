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

    private int assembleId; //该应用所属的assemble id

    private String assembleTag; //该应用所在的assemble（k8s pod）的tag

    private String appName;

    private String hostIp;

    private String port;

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

    public int getAssembleId() {
        return assembleId;
    }

    public void setAssembleId(int assembleId) {
        this.assembleId = assembleId;
    }

    public String getAssembleTag() {
        return assembleTag;
    }

    public void setAssembleTag(String assembleTag) {
        this.assembleTag = assembleTag;
    }
}
