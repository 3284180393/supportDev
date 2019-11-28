package com.channelsoft.ccod.support.cmdb.vo;

/**
 * @ClassName: AppParamVo
 * @Author: lanhb
 * @Description: 用来定义应用相关参数类
 * @Date: 2019/11/28 15:17
 * @Version: 1.0
 */
public class AppParamVo {

    private int method;   //处理方式:1、通过扫描nexus服务器自动添加，目前只支持1后续开发支持其它方式

    private String appName; //应用名

    private String appAlias; //应用别名

    private String version; //应用版本

    private Object data; //必要参数,data里面包含的数据由method绝定

    public int getMethod() {
        return method;
    }

    public void setMethod(int method) {
        this.method = method;
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
}
