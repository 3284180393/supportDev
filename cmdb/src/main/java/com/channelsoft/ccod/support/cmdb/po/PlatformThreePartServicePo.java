package com.channelsoft.ccod.support.cmdb.po;

/**
 * @ClassName: PlatformThreePartServicePo
 * @Author: lanhb
 * @Description: 用来定义平台第三方服务
 * @Date: 2020/6/3 16:49
 * @Version: 1.0
 */
public class PlatformThreePartServicePo {

    private int threeServiceId; //id，数据库主键

    private String platformId; //归属平台id

    private int assembleId; //该应用所属的assemble id

    private String assembleTag; //该应用所在的assemble（k8s pod）的tag

    private String serviceName; //服务名

    private String hostIp; //服务所在服务器ip

    private String port; //使用端口

    public int getThreeServiceId() {
        return threeServiceId;
    }

    public void setThreeServiceId(int threeServiceId) {
        this.threeServiceId = threeServiceId;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
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

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
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
}
