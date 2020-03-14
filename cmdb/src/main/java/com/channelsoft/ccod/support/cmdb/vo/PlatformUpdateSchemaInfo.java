package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.DatabaseType;
import com.channelsoft.ccod.support.cmdb.constant.PlatformUpdateTaskType;
import com.channelsoft.ccod.support.cmdb.constant.UpdateStatus;

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

    private List<DomainUpdatePlanInfo> domainUpdatePlanList; //域升级方案列表

    private String platformId; //平台id

    private String platformName; //该平台的平台名,需要同蓝鲸的对应的bizName一致

    private int bkBizId; //该平台对应蓝鲸的bizId

    private int bkCloudId; //该平台所有服务器所在云

    private String ccodVersion; //该平台的ccod大版本

    private PlatformUpdateTaskType taskType; //升级计划的任务类型,由PlatformUpdateTaskType枚举定义

    private UpdateStatus status; //任务当前状态,由PlatformUpdateTaskStatus枚举定义

    private Date createTime; //计划创建时间

    private Date updateTime; //计划最后一次修改时间

    private Date executeTime; //如果该时间为非空，在该时间自动执行

    private Date deadline; //计划完成最后期限

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

    public PlatformUpdateSchemaInfo()
    {

    }

    public PlatformUpdateSchemaInfo(String platformId, String platformName, int bkBizId, int bkCloudId, String ccodVersion,
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
        this.executeTime = now;
        this.deadline = now;
        this.title = title;
        this.comment = comment;
        this.domainUpdatePlanList = new ArrayList<>();
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

    public Date getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(Date executeTime) {
        this.executeTime = executeTime;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
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
}
