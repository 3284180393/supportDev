package com.channelsoft.ccod.support.cmdb.po;

/**
 * @ClassName: ServerPo
 * @Author: lanhb
 * @Description: 用来定义服务器信息
 * @Date: 2019/11/12 15:56
 * @Version: 1.0
 */
public class ServerPo {

    private int serverId;  //服务器id,数据库唯一生成主键

    private int serverType; //服务类型 1:linux,2:windows

    private String platformId; //服务所在平台

    private String domainId; //服务器所在域

    private String hostIp; //服务器ip

    private String hostname; //服务器名

    private int status; //服务器状态

    private String comment; //备注

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getServerType() {
        return serverType;
    }

    public void setServerType(int serverType) {
        this.serverType = serverType;
    }
}
