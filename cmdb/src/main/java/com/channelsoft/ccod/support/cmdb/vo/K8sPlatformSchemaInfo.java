package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.*;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @ClassName: K8sPlatformSchemaInfo
 * @Author: lanhb
 * @Description: 用来定义k8e平台规划
 * @Date: 2020/7/10 14:58
 * @Version: 1.0
 */
public class K8sPlatformSchemaInfo {

    @NotNull(message = "schema id can not be null")
    private String schemaId; //id由发起升级计划的生成的用来标识计划的唯一标识

    @NotNull(message = "domain plan can not be null")
    private List<K8sDomainPlanInfo> domainPlanList; //域升级方案列表

    @NotNull(message = "platform id can not be null")
    private String platformId; //平台id

    private String platformName; //该平台的平台名,需要同蓝鲸的对应的bizName一致

    private PlatformType platformType = PlatformType.K8S_CONTAINER; //平台类型

    private PlatformFunction platformFunc; //平台用途

    private PlatformCreateMethod createMethod; //平台创建方式

    private int bkBizId; //该平台对应蓝鲸的bizId

    private int bkCloudId; //该平台所有服务器所在云

    private String ccodVersion; //该平台的ccod大版本

    @NotNull(message = "type of platform schema can not be null")
    private PlatformUpdateTaskType taskType; //升级计划的任务类型,由PlatformUpdateTaskType枚举定义

    @NotNull(message = "status of platform schema can not be null")
    private UpdateStatus status; //任务当前状态,由PlatformUpdateTaskStatus枚举定义

    @NotNull(message = "title of platform schema can not be null")
    private String title; //升级任务标题

    @NotNull(message = "comment of platform schema can not be null")
    private String comment; //备注

    @NotNull(message = "k8s host ip can not be null")
    private String k8sHostIp; //运行平台的k8s主机ip

    @NotNull(message = "glsserver db type of platform can not be null")
    private DatabaseType glsDBType; //ccod平台glsserver的数据库类型

    @NotNull(message = "glsserver user of platform can not be null")
    private String glsDBUser; //gls数据库的db用户

    @NotNull(message = "glsserver user password of platform can not be null")
    private String glsDBPwd; //gls数据库的登录密码

    @NotNull(message = "public config of platform can not be null")
    private List<AppFileNexusInfo> publicConfig; //用来存放平台公共配置

    private List<K8sAppAssembleInfo> threeAppAssembleList;

    private List<V1PersistentVolume> pvList; //需要加载的pv列表

    private List<V1PersistentVolumeClaim> pvcList; //需要加载的pvc列表

    @NotNull(message = "k8s api url can not be null")
    private String k8sApiUrl; //k8s api的url地址

    @NotNull(message = "k8s auth token can not be null")
    private String k8sAuthToken; //k8s的认证token

    public String getSchemaId() {
        return schemaId;
    }

    public void setSchemaId(String schemaId) {
        this.schemaId = schemaId;
    }

    public List<K8sDomainPlanInfo> getDomainPlanList() {
        return domainPlanList;
    }

    public void setDomainPlanList(List<K8sDomainPlanInfo> domainPlanList) {
        this.domainPlanList = domainPlanList;
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

    public String getCcodVersion() {
        return ccodVersion;
    }

    public void setCcodVersion(String ccodVersion) {
        this.ccodVersion = ccodVersion;
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

    public String getK8sHostIp() {
        return k8sHostIp;
    }

    public void setK8sHostIp(String k8sHostIp) {
        this.k8sHostIp = k8sHostIp;
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

    public List<K8sAppAssembleInfo> getThreeAppAssembleList() {
        return threeAppAssembleList;
    }

    public void setThreeAppAssembleList(List<K8sAppAssembleInfo> threeAppAssembleList) {
        this.threeAppAssembleList = threeAppAssembleList;
    }

    public List<V1PersistentVolume> getPvList() {
        return pvList;
    }

    public void setPvList(List<V1PersistentVolume> pvList) {
        this.pvList = pvList;
    }

    public List<V1PersistentVolumeClaim> getPvcList() {
        return pvcList;
    }

    public void setPvcList(List<V1PersistentVolumeClaim> pvcList) {
        this.pvcList = pvcList;
    }
}
