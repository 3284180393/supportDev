package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.*;
import com.channelsoft.ccod.support.cmdb.k8s.vo.K8sThreePartAppVo;
import com.channelsoft.ccod.support.cmdb.k8s.vo.K8sThreePartServiceVo;
import com.channelsoft.ccod.support.cmdb.po.PlatformBase;
import com.google.gson.Gson;
import io.kubernetes.client.openapi.models.*;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * @ClassName: PlatformUpdateSchemaInfo
 * @Author: lanhb
 * @Description: 用来定义平台升级计划的类
 * @Date: 2019/12/11 17:47
 * @Version: 1.0
 */
@Validated
public class PlatformUpdateSchemaInfo extends PlatformBase {

    @NotNull(message = "schemaId can not be null")
    private String schemaId; //id由发起升级计划的生成的用来标识计划的唯一标识

    @Valid
    private List<DomainUpdatePlanInfo> domains; //域升级方案列表

    @NotNull(message = "taskType can not be null")
    private PlatformUpdateTaskType taskType; //升级计划的任务类型,由PlatformUpdateTaskType枚举定义

    @NotNull(message = "status can not be null")
    private UpdateStatus status; //任务当前状态,由PlatformUpdateTaskStatus枚举定义

    private String title; //升级任务标题

    private String comment; //备注

    private V1Namespace namespace; //命名空间

    private List<V1Secret> k8sSecrets;

    private V1Job k8sJob; //需要执行的k8s job

    private List<K8sThreePartAppVo> threePartApps;

    private List<K8sThreePartServiceVo> threePartServices;

    private String srcPlatformId;

    private Map<String, String> configCenterData; //用来定义配置中心化的所有（k,v）对

    private List<HostConfig> hosts; //用来定义主机相关配置

    private NginxConfig nginx;   //在主机上部署时，用来定义nginx配置

    private List<ThreePartAppConfig> depend; //用来定义依赖的第三应用相关配置

    public PlatformUpdateSchemaInfo() {}

    public PlatformUpdateSchemaInfo(PlatformBase platformBase, PlatformUpdateTaskType taskType, UpdateStatus status, String title, String comment)
    {
        super(platformBase);
        this.taskType = taskType;
        this.title = title;
        this.comment = comment;
        this.status = status;
        this.domains = new ArrayList<>();
        this.threePartApps = new ArrayList<>();
        this.configCenterData = new HashMap<>();
    }

    public PlatformUpdateSchemaInfo(PlatformBase platformBase, Map<String, Object> params, PlatformUpdateTaskType taskType, UpdateStatus status, String title, String comment)
    {
        super(platformBase, params);
        this.taskType = taskType;
        this.title = title;
        this.comment = comment;
        this.status = status;
        this.domains = new ArrayList<>();
        this.threePartApps = new ArrayList<>();
        this.threePartServices = new ArrayList<>();
        this.configCenterData = new HashMap<>();
    }

    public List<DomainUpdatePlanInfo> getDomains() {
        return domains;
    }

    public void setDomains(List<DomainUpdatePlanInfo> domains) {
        this.domains = domains;
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

    public String getSchemaId() {
        return schemaId;
    }

    public void setSchemaId(String schemaId) {
        this.schemaId = schemaId;
    }

    public V1Namespace getNamespace() {
        return namespace;
    }

    public void setNamespace(V1Namespace namespace) {
        this.namespace = namespace;
    }

    public List<K8sThreePartAppVo> getThreePartApps() {
        return threePartApps;
    }

    public void setThreePartApps(List<K8sThreePartAppVo> threePartApps) {
        this.threePartApps = threePartApps;
    }

    public List<V1Secret> getK8sSecrets() {
        return k8sSecrets;
    }

    public void setK8sSecrets(List<V1Secret> k8sSecrets) {
        this.k8sSecrets = k8sSecrets;
    }

    public V1Job getK8sJob() {
        return k8sJob;
    }

    public void setK8sJob(V1Job k8sJob) {
        this.k8sJob = k8sJob;
    }

    public List<K8sThreePartServiceVo> getThreePartServices() {
        return threePartServices;
    }

    public void setThreePartServices(List<K8sThreePartServiceVo> threePartServices) {
        this.threePartServices = threePartServices;
    }

    public String getSrcPlatformId() {
        return srcPlatformId;
    }

    public void setSrcPlatformId(String srcPlatformId) {
        this.srcPlatformId = srcPlatformId;
    }

    public Map<String, String> getConfigCenterData() {
        return configCenterData;
    }

    public void setConfigCenterData(Map<String, String> configCenterData) {
        this.configCenterData = configCenterData;
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

}
