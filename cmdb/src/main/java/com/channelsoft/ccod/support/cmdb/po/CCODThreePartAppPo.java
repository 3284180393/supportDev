package com.channelsoft.ccod.support.cmdb.po;

import org.apache.commons.lang3.StringUtils;
import java.util.Map;

/**
 * @ClassName: CCODThreePartAppPo
 * @Author: lanhb
 * @Description: 用来定义ccod平台和第三方应用的关系pojo类
 * @Date: 2020/11/18 17:01
 * @Version: 1.0
 */
public class CCODThreePartAppPo {

    private int id; //id数据库唯一主键

    private String ccodVersion; //用来定义ccod大版本

    private String tag; //一个ccod大版本可能有许多特定功能的小版本，tag用来区分这些小版本

    private String appName; //第三方应用名，例如mysql，oracle等

    private String alias; //该应用在平台中的唯一别名

    private String version; //应用版本

    private Map<String, String> params; //特定参数

    private String volume; //数据卷名

    private String mountSubPath; //挂载子目录

    public CCODThreePartAppPo()
    {}

    public CCODThreePartAppPo(String ccodVersion, String tag, String appName, String alias)
    {
        this.ccodVersion = ccodVersion;
        this.tag = StringUtils.isBlank(tag) ? "standard" : tag;
        this.appName = appName;
        this.alias = alias;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCcodVersion() {
        return ccodVersion;
    }

    public void setCcodVersion(String ccodVersion) {
        this.ccodVersion = ccodVersion;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = StringUtils.isBlank(tag) ? "standard" : tag;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getMountSubPath() {
        return mountSubPath;
    }

    public void setMountSubPath(String mountSubPath) {
        this.mountSubPath = mountSubPath;
    }
}
