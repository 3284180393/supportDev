package com.channelsoft.ccod.support.cmdb.po;

/**
 * @ClassName: AssemblePo
 * @Author: lanhb
 * @Description: 用来定义assemble，一个assemble映射到k8s就是pod
 * @Date: 2020/6/1 14:00
 * @Version: 1.0
 */
public class AssemblePo {

    private int assembleId; //id，数据库主键

    private String platformId; //对应的平台id

    private String domainId; //对应的域id

    private String tag; //assemble对应k8s的pod的标签

    private String status; //assemble对应k8s的pod状态

    public int getAssembleId() {
        return assembleId;
    }

    public void setAssembleId(int assembleId) {
        this.assembleId = assembleId;
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

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
