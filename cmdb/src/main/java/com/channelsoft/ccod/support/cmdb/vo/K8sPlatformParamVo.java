package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.po.DomainPublicConfigPo;
import com.channelsoft.ccod.support.cmdb.po.PlatformPublicConfigPo;
import com.channelsoft.ccod.support.cmdb.po.PlatformThreePartAppPo;
import com.channelsoft.ccod.support.cmdb.po.PlatformThreePartServicePo;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: K8sPlatformParamVo
 * @Author: lanhb
 * @Description: 用来定义k8s平台相关参数
 * @Date: 2020/6/20 17:29
 * @Version: 1.0
 */
public class K8sPlatformParamVo {

    String platformId;

    String platformName;

    List<PlatformAppDeployDetailVo> deployAppList;

    List<PlatformThreePartAppPo> threeAppList;

    List<PlatformThreePartServicePo> threeSvcList;

    List<PlatformPublicConfigPo> platformPublicConfigList;

    List<DomainPublicConfigPo> domainPublicConfigList;

    public K8sPlatformParamVo(String platformId, String platformName)
    {
        this.platformId = platformId;
        this.platformName = platformName;
        this.deployAppList = new ArrayList<>();
        this.threeAppList = new ArrayList<>();
        this.threeSvcList = new ArrayList<>();
        this.platformPublicConfigList = new ArrayList<>();
        this.domainPublicConfigList = new ArrayList<>();
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public List<PlatformAppDeployDetailVo> getDeployAppList() {
        return deployAppList;
    }

    public void setDeployAppList(List<PlatformAppDeployDetailVo> deployAppList) {
        this.deployAppList = deployAppList;
    }

    public List<PlatformThreePartAppPo> getThreeAppList() {
        return threeAppList;
    }

    public void setThreeAppList(List<PlatformThreePartAppPo> threeAppList) {
        this.threeAppList = threeAppList;
    }

    public List<PlatformThreePartServicePo> getThreeSvcList() {
        return threeSvcList;
    }

    public void setThreeSvcList(List<PlatformThreePartServicePo> threeSvcList) {
        this.threeSvcList = threeSvcList;
    }

    public List<PlatformPublicConfigPo> getPlatformPublicConfigList() {
        return platformPublicConfigList;
    }

    public void setPlatformPublicConfigList(List<PlatformPublicConfigPo> platformPublicConfigList) {
        this.platformPublicConfigList = platformPublicConfigList;
    }

    public List<DomainPublicConfigPo> getDomainPublicConfigList() {
        return domainPublicConfigList;
    }

    public void setDomainPublicConfigList(List<DomainPublicConfigPo> domainPublicConfigList) {
        this.domainPublicConfigList = domainPublicConfigList;
    }
}
