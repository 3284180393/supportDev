package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.config.BizSetDefine;
import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.constant.AppUpdateOperation;
import com.channelsoft.ccod.support.cmdb.constant.VersionControl;
import com.channelsoft.ccod.support.cmdb.po.*;
import io.kubernetes.client.openapi.models.V1ConfigMap;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName: PlatformAppDeployDetailVo
 * @Author: lanhb
 * @Description: 用来定义平台应用部署情况
 * @Date: 2019/11/25 17:17
 * @Version: 1.0
 */
public class PlatformAppDeployDetailVo extends AppBase {

    private int platformAppId; //平台app部署id,数据库唯一生成

    private int appId; //应用id,外键app的appId

    private String platformId; //平台id

    private String platformName; //部署平台名

    private String domainId; //应用所在的域id,外键domain的domain_id

    private String domainName; //部署域名

    private int assembleId; //应用所在assemble的id

    private String assembleTag; //应用所在assemble的tag

    private String originalAlias; //原始别名

    private String hostname; //部署服务器名

    private String hostIp; //服务器ip

    private boolean fixedIp; //ip是否为固定

    private Integer replicas; //运行副本数目

    private Integer availableReplicas; //可用副本数目

    private String status; //应用的相关status

    private String appRunner; //运行用户名

    private Date deployTime; //该应用的部署路径

    private Date createTime; //应用创建时间

    private String versionControl; //版本控制方式

    private String versionControlUrl; //版本控制的连接url

    private int bkBizId; //该应用在蓝鲸biz id

    private int bkSetId; //该应用在paas的set id

    private String bkSetName; //该应用在paas的set名字

    private int bkModuleId; //该应用在蓝鲸paas的唯一id

    private int bkHostId; //该应用在蓝鲸paas的服务器id

    private String tag; //应用标签

    public PlatformAppDeployDetailVo()
    {}

    public PlatformAppDeployDetailVo(AppBase appBase)
    {
        super(appBase);
    }

    public int getPlatformAppId() {
        return platformAppId;
    }

    public void setPlatformAppId(int platformAppId) {
        this.platformAppId = platformAppId;
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
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

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Date getDeployTime() {
        return deployTime;
    }

    public void setDeployTime(Date deployTime) {
        this.deployTime = deployTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getVersionControl() {
        return versionControl;
    }

    public void setVersionControl(String versionControl) {
        this.versionControl = versionControl;
    }

    public String getVersionControlUrl() {
        return versionControlUrl;
    }

    public void setVersionControlUrl(String versionControlUrl) {
        this.versionControlUrl = versionControlUrl;
    }

    public String getAppRunner() {
        return appRunner;
    }

    public void setAppRunner(String appRunner) {
        this.appRunner = appRunner;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public int getBkModuleId() {
        return bkModuleId;
    }

    public void setBkModuleId(int bkModuleId) {
        this.bkModuleId = bkModuleId;
    }

    public int getBkBizId() {
        return bkBizId;
    }

    public void setBkBizId(int bkBizId) {
        this.bkBizId = bkBizId;
    }

    public int getBkSetId() {
        return bkSetId;
    }

    public void setBkSetId(int bkSetId) {
        this.bkSetId = bkSetId;
    }

    public int getBkHostId() {
        return bkHostId;
    }

    public void setBkHostId(int bkHostId) {
        this.bkHostId = bkHostId;
    }

    public String getBkSetName() {
        return bkSetName;
    }

    public void setBkSetName(String bkSetName) {
        this.bkSetName = bkSetName;
    }

    public String getOriginalAlias() {
        return originalAlias;
    }

    public void setOriginalAlias(String originalAlias) {
        this.originalAlias = originalAlias;
    }

    public int getAssembleId() {
        return assembleId;
    }

    public void setAssembleId(int assembleId) {
        this.assembleId = assembleId;
    }

    public String getAssembleTag() {
        return assembleTag;
    }

    public void setAssembleTag(String assembleTag) {
        this.assembleTag = assembleTag;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }

    public Integer getAvailableReplicas() {
        return availableReplicas;
    }

    public void setAvailableReplicas(Integer availableReplicas) {
        this.availableReplicas = availableReplicas;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public boolean isFixedIp() {
        return fixedIp;
    }

    public void setFixedIp(boolean fixedIp) {
        this.fixedIp = fixedIp;
    }

    public PlatformAppPo getPlatformApp()
    {
        PlatformAppPo po = new PlatformAppPo(this, appId, cfgs, platformId, domainId, assembleId, originalAlias, hostIp,  fixedIp, appRunner, tag);
        po.setPlatformAppId(this.platformAppId);
        return po;
    }

    public AppUpdateOperationInfo getOperationInfo(AppUpdateOperation operation)
    {
        AppUpdateOperationInfo optInfo = new AppUpdateOperationInfo(this);
        optInfo.setDomainId(this.domainId);
        optInfo.setAssembleTag(this.getAssembleTag());
        optInfo.setFixedIp(fixedIp);
        optInfo.setHostIp(this.hostIp);
        optInfo.setCfgs(this.getCfgs().stream().map(cfg->cfg.getAppFileNexusInfo()).collect(Collectors.toList()));
        optInfo.setAppRunner(this.appRunner);
        optInfo.setDomainName(this.domainName);
        optInfo.setTag(tag);
        optInfo.setOperation(operation);
        optInfo.setOriginalVersion(this.originalAlias);
        optInfo.setOriginalAlias(this.originalAlias);
        return optInfo;
    }

    public AppUpdateOperationInfo getOperationInfo(AppUpdateOperation operation, String originalVersion)
    {
        AppUpdateOperationInfo optInfo = getOperationInfo(operation);
        optInfo.setOriginalVersion(originalVersion);
        return optInfo;
    }

    public String getCfgNexusDirectory(String tag)
    {
        return String.format(String.format("%s/%s/%s/%s/%s/%s", platformId, tag, domainId, appName, alias, version));
    }

    public String getCfgNexusDirectory(Date date)
    {
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        return getCfgNexusDirectory(sf.format(date));
    }
}
