package com.channelsoft.ccod.support.cmdb.vo;

/**
 * @ClassName: CfgQueryParamVo
 * @Author: lanhb
 * @Description: 用来定义应用配置文件查询参数
 * @Date: 2020/4/1 18:24
 * @Version: 1.0
 */
public class CfgQueryParamVo {

    private String appName;  //应用名

    private String version; //版本号

    private String cfgFileName; //希望查询的配置文件名

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

    public String getCfgFileName() {
        return cfgFileName;
    }

    public void setCfgFileName(String cfgFileName) {
        this.cfgFileName = cfgFileName;
    }
}
