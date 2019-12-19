package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.po.*;

import java.util.Map;

/**
 * @ClassName: PlatformAppParamVo
 * @Author: lanhb
 * @Description: 用来定义平台/app相关参数的类
 * @Date: 2019/11/24 10:10
 * @Version: 1.0
 */
public class PlatformAppParamVo {

    private int method; //平台应用处理方式,1、通过通知平台客户端自动扫描的方式添加,目前只支持1

    private String platformId; //平台id

    private String platformName; //平台名

    private String domainId; //域id

    private String hostIp; //主机ip

    private String appName; //应用名

    private String version; //版本

    private Object data; //同平台应用相关的数据,data包含的数据类型由method决定

    public int getMethod() {
        return method;
    }

    public void setMethod(int method) {
        this.method = method;
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

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }
}
