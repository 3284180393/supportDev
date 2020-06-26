package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.*;
import com.channelsoft.ccod.support.cmdb.po.PlatformPo;
import io.kubernetes.client.openapi.models.V1Deployment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @ClassName: PlatformUpdateSchemaInfo
 * @Author: lanhb
 * @Description: 用来定义平台升级计划的类
 * @Date: 2019/12/11 17:47
 * @Version: 1.0
 */
public class PlatformUpdateSchemaInfo {

    private String schemaId; //id由发起升级计划的生成的用来标识计划的唯一标识

    private List<DomainUpdatePlanInfo> domainUpdatePlanList; //域升级方案列表

    private String platformId; //平台id

    private String platformName; //该平台的平台名,需要同蓝鲸的对应的bizName一致

    private PlatformType platformType; //平台类型

    private PlatformFunction platformFunc; //平台用途

    private PlatformCreateMethod createMethod; //平台创建方式

    private int bkBizId; //该平台对应蓝鲸的bizId

    private int bkCloudId; //该平台所有服务器所在云

    private String ccodVersion; //该平台的ccod大版本

    private PlatformUpdateTaskType taskType; //升级计划的任务类型,由PlatformUpdateTaskType枚举定义

    private UpdateStatus status; //任务当前状态,由PlatformUpdateTaskStatus枚举定义

    private Date createTime; //计划创建时间

    private Date updateTime; //计划最后一次修改时间

    private String title; //升级任务标题

    private String comment; //备注

    private String deployScriptRepository; //平台升级计划部署脚本在nexus的存储仓库

    private String deployScriptPath; //平台升级计划部署脚本的path

    private String deployScriptMd5;  //平台升级计划的md5

    private String k8sHostIp; //运行平台的k8s主机ip

    private DatabaseType glsDBType; //ccod平台glsserver的数据库类型

    private String glsDBUser; //gls数据库的db用户

    private String glsDBPwd; //gls数据库的登录密码

    private String baseDataNexusRepository; //基础数据在nexus的存放仓库

    private String baseDataNexusPath; //基础数据在nexus的存放path

    private List<AppFileNexusInfo> publicConfig; //用来存放平台公共配置

    private String k8sApiUrl; //k8s api的url地址

    private String k8sAuthToken; //k8s的认证token

    private List<V1Deployment> k8sDeploymentList; //需要执行的deployment

    public PlatformUpdateSchemaInfo()
    {

    }

    public PlatformUpdateSchemaInfo(String platformId, String platformName, PlatformType platformType, PlatformFunction platformFunc,
                                    PlatformCreateMethod createMethod, int bkBizId, int bkCloudId, String ccodVersion,
                                    PlatformUpdateTaskType taskType, String title, String comment)
    {
        this.platformId = platformId;
        this.platformName = platformName;
        this.bkBizId = bkBizId;
        this.bkCloudId = bkCloudId;
        this.ccodVersion = ccodVersion;
        this.taskType = taskType;
        this.status = UpdateStatus.CREATE;
        Date now = new Date();
        this.createTime = now;
        this.updateTime = now;
        this.title = title;
        this.comment = comment;
        this.domainUpdatePlanList = new ArrayList<>();
        this.platformType = platformType;
        this.platformFunc = platformFunc;
        this.createMethod = createMethod;
        this.k8sDeploymentList = new ArrayList<>();
    }

    public List<DomainUpdatePlanInfo> getDomainUpdatePlanList() {
        return domainUpdatePlanList;
    }

    public void setDomainUpdatePlanList(List<DomainUpdatePlanInfo> domainUpdatePlanList) {
        this.domainUpdatePlanList = domainUpdatePlanList;
    }

    public PlatformUpdateTaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(PlatformUpdateTaskType taskType) {
        this.taskType = taskType;
    }

    public UpdateStatus getStatus() {
        return status;
    }

    public void setStatus(UpdateStatus status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getBkBizId() {
        return bkBizId;
    }

    public void setBkBizId(int bkBizId) {
        this.bkBizId = bkBizId;
    }

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

    public int getBkCloudId() {
        return bkCloudId;
    }

    public void setBkCloudId(int bkCloudId) {
        this.bkCloudId = bkCloudId;
    }

    public String getCcodVersion() {
        return ccodVersion;
    }

    public void setCcodVersion(String ccodVersion) {
        this.ccodVersion = ccodVersion;
    }

    public String getDeployScriptRepository() {
        return deployScriptRepository;
    }

    public void setDeployScriptRepository(String deployScriptRepository) {
        this.deployScriptRepository = deployScriptRepository;
    }

    public String getDeployScriptPath() {
        return deployScriptPath;
    }

    public void setDeployScriptPath(String deployScriptPath) {
        this.deployScriptPath = deployScriptPath;
    }

    public String getDeployScriptMd5() {
        return deployScriptMd5;
    }

    public void setDeployScriptMd5(String deployScriptMd5) {
        this.deployScriptMd5 = deployScriptMd5;
    }

    public DatabaseType getGlsDBType() {
        return glsDBType;
    }

    public void setGlsDBType(DatabaseType glsDBType) {
        this.glsDBType = glsDBType;
    }

    public String getGlsDBUser() {
        return glsDBUser;
    }

    public void setGlsDBUser(String glsDBUser) {
        this.glsDBUser = glsDBUser;
    }

    public String getGlsDBPwd() {
        return glsDBPwd;
    }

    public void setGlsDBPwd(String glsDBPwd) {
        this.glsDBPwd = glsDBPwd;
    }

    public String getBaseDataNexusRepository() {
        return baseDataNexusRepository;
    }

    public void setBaseDataNexusRepository(String baseDataNexusRepository) {
        this.baseDataNexusRepository = baseDataNexusRepository;
    }

    public String getBaseDataNexusPath() {
        return baseDataNexusPath;
    }

    public void setBaseDataNexusPath(String baseDataNexusPath) {
        this.baseDataNexusPath = baseDataNexusPath;
    }

    public String getK8sHostIp() {
        return k8sHostIp;
    }

    public void setK8sHostIp(String k8sHostIp) {
        this.k8sHostIp = k8sHostIp;
    }

    public String getSchemaId() {
        return schemaId;
    }

    public void setSchemaId(String schemaId) {
        this.schemaId = schemaId;
    }

    public List<AppFileNexusInfo> getPublicConfig() {
        return publicConfig;
    }

    public void setPublicConfig(List<AppFileNexusInfo> publicConfig) {
        this.publicConfig = publicConfig;
    }

    public String getK8sApiUrl() {
        return k8sApiUrl;
    }

    public void setK8sApiUrl(String k8sApiUrl) {
        this.k8sApiUrl = k8sApiUrl;
    }

    public String getK8sAuthToken() {
        return k8sAuthToken;
    }

    public void setK8sAuthToken(String k8sAuthToken) {
        this.k8sAuthToken = k8sAuthToken;
    }

    public PlatformType getPlatformType() {
        return platformType;
    }

    public void setPlatformType(PlatformType platformType) {
        this.platformType = platformType;
    }

    public PlatformFunction getPlatformFunc() {
        return platformFunc;
    }

    public void setPlatformFunc(PlatformFunction platformFunc) {
        this.platformFunc = platformFunc;
    }

    public PlatformCreateMethod getCreateMethod() {
        return createMethod;
    }

    public void setCreateMethod(PlatformCreateMethod createMethod) {
        this.createMethod = createMethod;
    }

    public List<V1Deployment> getK8sDeploymentList() {
        return k8sDeploymentList;
    }

    public void setK8sDeploymentList(List<V1Deployment> k8sDeploymentList) {
        this.k8sDeploymentList = k8sDeploymentList;
    }
}
