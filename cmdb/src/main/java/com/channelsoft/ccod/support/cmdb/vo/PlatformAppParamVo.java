package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.po.*;

import java.util.Map;

/**
 * @ClassName: PlatformAppParamVo
 * @Author: lanhb
 * @Description: 同平台平台应用处理相关的参数
 * @Date: 2019/11/24 10:10
 * @Version: 1.0
 */
public class PlatformAppParamVo {
    public String platformId;
    public String appRepository;
    public String cfgRepository;
    public Map<String, AppPo> appMap;
    public Map<String, PlatformPo> platformMap;
    public Map<String, DomainPo> domainMap;
    public Map<String, ServerPo> serverMap;
    public Map<String, ServerUserPo> serverUserMap;
    public Map<String, Map<String, NexusAssetInfo>> appFileAssetMap;
    public Map<String, Map<String, NexusAssetInfo>> cfgFileAssetMap;

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getAppRepository() {
        return appRepository;
    }

    public void setAppRepository(String appRepository) {
        this.appRepository = appRepository;
    }

    public String getCfgRepository() {
        return cfgRepository;
    }

    public void setCfgRepository(String cfgRepository) {
        this.cfgRepository = cfgRepository;
    }

    public Map<String, AppPo> getAppMap() {
        return appMap;
    }

    public void setAppMap(Map<String, AppPo> appMap) {
        this.appMap = appMap;
    }

    public Map<String, PlatformPo> getPlatformMap() {
        return platformMap;
    }

    public void setPlatformMap(Map<String, PlatformPo> platformMap) {
        this.platformMap = platformMap;
    }

    public Map<String, DomainPo> getDomainMap() {
        return domainMap;
    }

    public void setDomainMap(Map<String, DomainPo> domainMap) {
        this.domainMap = domainMap;
    }

    public Map<String, ServerPo> getServerMap() {
        return serverMap;
    }

    public void setServerMap(Map<String, ServerPo> serverMap) {
        this.serverMap = serverMap;
    }

    public Map<String, ServerUserPo> getServerUserMap() {
        return serverUserMap;
    }

    public void setServerUserMap(Map<String, ServerUserPo> serverUserMap) {
        this.serverUserMap = serverUserMap;
    }

    public Map<String, Map<String, NexusAssetInfo>> getAppFileAssetMap() {
        return appFileAssetMap;
    }

    public void setAppFileAssetMap(Map<String, Map<String, NexusAssetInfo>> appFileAssetMap) {
        this.appFileAssetMap = appFileAssetMap;
    }

    public Map<String, Map<String, NexusAssetInfo>> getCfgFileAssetMap() {
        return cfgFileAssetMap;
    }

    public void setCfgFileAssetMap(Map<String, Map<String, NexusAssetInfo>> cfgFileAssetMap) {
        this.cfgFileAssetMap = cfgFileAssetMap;
    }
}
