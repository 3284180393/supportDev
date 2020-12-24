package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.constant.K8sKind;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
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

    private K8sKind kind;  //提供服务方式，目前只支持POD和ENDPOINTS，POD：通过本地pod提供服务,ENDPOINTS通过endpoints提供服务

    private Map<String, String> params; //特定参数

    private String volume; //数据卷名

    private String mountSubPath; //挂载子目录

    private int timeout;  //deployment启动超时时长

    private Map<String, String> cfgs; //相关配置

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

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public Map<String, String> getCfgs() {
        return cfgs;
    }

    public void setCfgs(Map<String, String> cfgs) {
        this.cfgs = cfgs;
    }

    public K8sKind getKind() {
        return kind;
    }

    public void setKind(K8sKind kind) {
        this.kind = kind;
    }

    public Map<String, String> getK8sMacroData(PlatformPo platform){
        Map<String, String> data = new HashMap<>();
        data.put(K8sObjectTemplatePo.PLATFORM_ID, platform.getPlatformId());
        data.put(K8sObjectTemplatePo.NFS_SERVER_IP, platform.getNfsServerIp() == null ? (String)platform.getParams().get(PlatformBase.nfsServerIpKey) : platform.getNfsServerIp());
        data.put(K8sObjectTemplatePo.K8S_HOST_IP, platform.getK8sHostIp() == null ? (String)platform.getParams().get(PlatformBase.k8sHostIpKey) : platform.getK8sHostIp());
        data.put(K8sObjectTemplatePo.HOST_URL, platform.getHostUrl());
        data.put(K8sObjectTemplatePo.APP_NAME, appName);
        data.put(K8sObjectTemplatePo.APP_LOW_NAME, appName.toLowerCase());
        data.put(K8sObjectTemplatePo.ALIAS, alias);
        if(StringUtils.isNotBlank(version)){
            data.put(K8sObjectTemplatePo.APP_VERSION, version);
        }
        if(cfgs != null && cfgs.size() > 0){
            cfgs.forEach((k,v)->data.put(String.format("${%s.%s}", appName, k).toUpperCase(), v));
        }
        return data;
    }
}
