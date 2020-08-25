package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.CCODPlatformStatus;
import com.channelsoft.ccod.support.cmdb.constant.PlatformCreateMethod;
import com.channelsoft.ccod.support.cmdb.constant.PlatformFunction;
import com.channelsoft.ccod.support.cmdb.constant.PlatformType;
import com.channelsoft.ccod.support.cmdb.po.PlatformPo;
import com.channelsoft.ccod.support.cmdb.po.PlatformThreePartAppPo;
import com.channelsoft.ccod.support.cmdb.po.PlatformThreePartServicePo;
import com.channelsoft.ccod.support.cmdb.po.UnconfirmedAppModulePo;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: PlatformTopologyInfo
 * @Author: lanhb
 * @Description: 用来定义平台拓扑信息
 * @Date: 2019/12/25 17:52
 * @Version: 1.0
 */
public class PlatformTopologyInfo {

    private String platformId; //平台id

    private String platformName; //平台名,同时对应蓝鲸paas的biz name

    private String ccodVersion; //该平台对应的ccod大版本

    private int bkBizId; //该平台在蓝鲸paas的biz id

    private int bkCloudId; //该平台下面所有host所在的cloud id

    private CCODPlatformStatus status; //该平台的状态

    private PlatformType type;  //平台类型

    private String apiUrl; //如果有查询平台状态的api，查询api的url，例如k8s的restful api的url

    private String authToken; //查询apiUrl所需的认证token

    private List<CCODSetInfo> setList; //平台下的所有集群

    private List<CCODHostInfo> idleHostList; //平台下的所有空闲服务器列表

    private List<PlatformThreePartAppPo> threePartAppList; //平台所使用的第三方应用列表

    private List<PlatformThreePartServicePo> threePartServiceList; //平台所依赖的第三方服务列表

    private PlatformUpdateSchemaInfo schema;  //平台的升级计划

    private List<UnconfirmedAppModulePo> unconfirmedAppModuleList; //平台中无法正确处理的应用模块

    private PlatformFunction func; //平台用途

    private PlatformCreateMethod createMethod; //平台创建方式

    public PlatformTopologyInfo(PlatformPo platform)
    {
        this.platformId = platform.getPlatformId();
        this.platformName = platform.getPlatformName();
        this.bkBizId = platform.getBkBizId();
        this.bkCloudId = platform.getBkCloudId();
        this.status = platform.getStatus();
        this.setList = new ArrayList<>();
        this.idleHostList = new ArrayList<>();
        this.ccodVersion = platform.getCcodVersion();
        this.type = platform.getType();
        this.apiUrl = platform.getK8sApiUrl();
        this.authToken = platform.getK8sAuthToken();
        this.threePartAppList = new ArrayList<>();
        this.threePartServiceList = new ArrayList<>();
        this.func = platform.getFunc();
        this.createMethod = platform.getCreateMethod();
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

    public int getBkBizId() {
        return bkBizId;
    }

    public void setBkBizId(int bkBizId) {
        this.bkBizId = bkBizId;
    }

    public int getBkCloudId() {
        return bkCloudId;
    }

    public void setBkCloudId(int bkCloudId) {
        this.bkCloudId = bkCloudId;
    }

    public CCODPlatformStatus getStatus() {
        return status;
    }

    public void setStatus(CCODPlatformStatus status) {
        this.status = status;
    }

    public List<CCODSetInfo> getSetList() {
        return setList;
    }

    public void setSetList(List<CCODSetInfo> setList) {
        this.setList = setList;
    }

    public List<CCODHostInfo> getIdleHostList() {
        return idleHostList;
    }

    public void setIdleHostList(List<CCODHostInfo> idleHostList) {
        this.idleHostList = idleHostList;
    }

    public PlatformUpdateSchemaInfo getSchema() {
        return schema;
    }

    public void setSchema(PlatformUpdateSchemaInfo schema) {
        this.schema = schema;
    }

    public String getCcodVersion() {
        return ccodVersion;
    }

    public void setCcodVersion(String ccodVersion) {
        this.ccodVersion = ccodVersion;
    }

    public PlatformType getType() {
        return type;
    }

    public void setType(PlatformType type) {
        this.type = type;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public void setIdleHosts(List<LJHostInfo> idleHostList) {

        this.idleHostList = new ArrayList<>();
        for(LJHostInfo hostInfo : idleHostList)
        {
            CCODHostInfo host = new CCODHostInfo(hostInfo);
            this.idleHostList.add(host);
        }
    }

    public List<UnconfirmedAppModulePo> getUnconfirmedAppModuleList() {
        return unconfirmedAppModuleList;
    }

    public void setUnconfirmedAppModuleList(List<UnconfirmedAppModulePo> unconfirmedAppModuleList) {
        this.unconfirmedAppModuleList = unconfirmedAppModuleList;
    }

    public List<PlatformThreePartAppPo> getThreePartAppList() {
        return threePartAppList;
    }

    public void setThreePartAppList(List<PlatformThreePartAppPo> threePartAppList) {
        this.threePartAppList = threePartAppList;
    }

    public List<PlatformThreePartServicePo> getThreePartServiceList() {
        return threePartServiceList;
    }

    public void setThreePartServiceList(List<PlatformThreePartServicePo> threePartServiceList) {
        this.threePartServiceList = threePartServiceList;
    }

    public PlatformFunction getFunc() {
        return func;
    }

    public void setFunc(PlatformFunction func) {
        this.func = func;
    }

    public PlatformCreateMethod getCreateMethod() {
        return createMethod;
    }

    public void setCreateMethod(PlatformCreateMethod createMethod) {
        this.createMethod = createMethod;
    }
}
