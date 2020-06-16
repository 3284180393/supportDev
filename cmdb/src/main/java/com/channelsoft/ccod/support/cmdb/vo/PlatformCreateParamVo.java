package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.*;
import com.channelsoft.ccod.support.cmdb.po.PlatformPo;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @ClassName: PlatformCreateParamVo
 * @Author: lanhb
 * @Description: 平台创建参数
 * @Date: 2020/2/18 22:02
 * @Version: 1.0
 */
public class PlatformCreateParamVo implements Serializable {

    @NotNull(message = "schemaId can not be null")
    private String schemaId; //平台升级计划id，用来唯一标识该计划的id

    @NotNull(message = "createMethod can not be null")
    private PlatformCreateMethod createMethod; //平台创建方式

    @NotNull(message = "platformId can not be null")
    private String platformId; //新建平台id

    @NotNull(message = "platformName can not be null")
    private String platformName; //新建平台名

    @NotNull(message = "platformType can not be null")
    private PlatformType platformType; //平台类型

    @NotNull(message = "platformFunc can not be null")
    private PlatformFunction platformFunc; //平台用途

    @NotNull(message = "bkBizId can not be null")
    private Integer bkBizId; //新建平台的biz id

    @NotNull(message = "bkCloudId can not be null")
    private Integer bkCloudId; //新建平台服务器所在的cloud id

    @NotNull(message = "ccodVersion can not be null")
    private String ccodVersion; //ccod大版本号

    private String params; //同平台创建相关的参数

    @NotNull(message = "k8sHostIp can not be null")
    private String k8sHostIp; //运行平台的k8s主机ip

    @NotNull(message = "glsDBType can not be null")
    private DatabaseType glsDBType; //ccod平台glsserver的数据库类型

    @NotNull(message = "glsDBUser can not be null")
    private String glsDBUser; //gls数据库的db用户

    @NotNull(message = "ccodVersion can not be null")
    private String glsDBPwd; //gls数据库的登录密码

    @NotNull(message = "baseDataNexusRepository can not be null")
    private String baseDataNexusRepository; //基础数据在nexus的存放仓库

    @NotNull(message = "baseDataNexusPath can not be null")
    private String baseDataNexusPath; //基础数据在nexus的存放path

    @NotNull(message = "publicConfig can not be null")
    private List<AppFileNexusInfo> publicConfig; //平台公共配置

    private String k8sApiUrl; //如果平台是部署在k8s上，需要指明k8s api的url

    private String k8sAuthToken; //如果平台是部署在k8s上，需要指明访问k8s api的认证token

    public PlatformCreateMethod getCreateMethod() {
        return createMethod;
    }

    public void setCreateMethod(PlatformCreateMethod createMethod) {
        this.createMethod = createMethod;
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

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
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

    public String getCcodVersion() {
        return ccodVersion;
    }

    public void setCcodVersion(String ccodVersion) {
        this.ccodVersion = ccodVersion;
    }

    public PlatformPo getCreatePlatform()
    {
        String desc;
        switch (createMethod)
        {
            case CLONE:
                desc = String.format("create by %s %s", createMethod.name, params);
                break;
            default:
                desc = String.format("create by %s", createMethod.name);
        }
        PlatformPo po = new PlatformPo(platformId, platformName, bkBizId, bkCloudId, CCODPlatformStatus.SCHEMA_CREATE_PLATFORM,
                ccodVersion, desc, platformType, platformFunc, createMethod);
        if(PlatformType.K8S_CONTAINER.equals(platformType))
        {
            po.setAuthToken(k8sAuthToken);
            po.setApiUrl(k8sApiUrl);
        }
        return po;
    }

    public PlatformUpdateSchemaInfo getPlatformCreateSchema(List<DomainUpdatePlanInfo> domainPlanList)
    {
        Date now = new Date();
        String title = String.format("%s[%s]新建计划", platformName, platformId);
        String comment = PlatformCreateMethod.CLONE.equals(createMethod) ? String.format("create by %s %s", createMethod.name, params) : String.format("create by %s", createMethod.name);
        PlatformUpdateSchemaInfo schema = new PlatformUpdateSchemaInfo();
        schema.setCcodVersion(ccodVersion);
        schema.setTitle(title);
        schema.setBkBizId(bkBizId);
        schema.setUpdateTime(now);
        schema.setPlatformId(platformId);
        schema.setTaskType(PlatformUpdateTaskType.CREATE);
        schema.setTitle(title);
        schema.setDomainUpdatePlanList(domainPlanList);
        schema.setStatus(UpdateStatus.CREATE);
        schema.setCreateTime(now);
        schema.setComment(comment);
        schema.setPlatformName(platformName);
        schema.setGlsDBType(glsDBType);
        schema.setK8sHostIp(k8sHostIp);
        schema.setGlsDBUser(glsDBUser);
        schema.setGlsDBPwd(glsDBPwd);
        schema.setBaseDataNexusPath(baseDataNexusPath);
        schema.setBaseDataNexusRepository(baseDataNexusRepository);
        schema.setBkCloudId(bkCloudId);
        schema.setSchemaId(schemaId);
        schema.setPublicConfig(publicConfig);
        schema.setK8sApiUrl(k8sApiUrl);
        schema.setK8sAuthToken(k8sAuthToken);
        schema.setPlatformFunc(platformFunc);
        schema.setPlatformType(platformType);
        schema.setCreateMethod(createMethod);
        return schema;
    }
}
