package com.channelsoft.ccod.support.cmdb.vo;

import java.util.List;
import java.util.Map;

/**
 * @ClassName: YamlSchemaVo
 * @Author: lanhb
 * @Description: 用来输出schema的yaml文件的辅助类
 * @Date: 2020/12/11 16:11
 * @Version: 1.0
 */
public class YamlSchemaVo {
    private String platformName;

    private String platformId;

    private String ccodVersion;

    private String hostUrl;

    private List<HostConfig> hosts; //用来定义主机相关配置

    private NginxConfig nginx;   //在主机上部署时，用来定义nginx配置

    private List<ThreePartAppConfig> depend; //用来定义依赖的第三应用相关配置

    private List<YamlDomainVo> domains;

    private Map<String, String> configCenterData; //用来定义配置中心化的所有（k,v）对

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getCcodVersion() {
        return ccodVersion;
    }

    public void setCcodVersion(String ccodVersion) {
        this.ccodVersion = ccodVersion;
    }

    public String getHostUrl() {
        return hostUrl;
    }

    public void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
    }

    public List<HostConfig> getHosts() {
        return hosts;
    }

    public void setHosts(List<HostConfig> hosts) {
        this.hosts = hosts;
    }

    public NginxConfig getNginx() {
        return nginx;
    }

    public void setNginx(NginxConfig nginx) {
        this.nginx = nginx;
    }

    public List<ThreePartAppConfig> getDepend() {
        return depend;
    }

    public void setDepend(List<ThreePartAppConfig> depend) {
        this.depend = depend;
    }

    public Map<String, String> getConfigCenterData() {
        return configCenterData;
    }

    public void setConfigCenterData(Map<String, String> configCenterData) {
        this.configCenterData = configCenterData;
    }

    public List<YamlDomainVo> getDomains() {
        return domains;
    }

    public void setDomains(List<YamlDomainVo> domains) {
        this.domains = domains;
    }
}
