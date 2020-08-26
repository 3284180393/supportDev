package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.constant.*;
import com.channelsoft.ccod.support.cmdb.vo.AppFileNexusInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: PlatformBase
 * @Author: lanhb
 * @Description: 用来定义平台基础属性的基类
 * @Date: 2020/8/25 15:24
 * @Version: 1.0
 */
public abstract class PlatformBase {

    @Value("${ccod.key.glsDBType}")
    protected String glsDBTypeKey;

    @Value("${ccod.key.glsDBUser}")
    protected String glsDBUserKey;

    @Value("${ccod.key.glsDBPwd}")
    protected String glsDBPwdKey;

    @Value("${ccod.key.glsDBSid}")
    protected String glsDBSidKey;

    @Value("${ccod.key.k8sHostIpKey}")
    protected String k8sHostIpKey;

    @Value("${ccod.key.baseDataNexusRepository}")
    protected String baseDataNexusRepositoryKey;

    @Value("${ccod.key.baseDataNexusPath}")
    protected String baseDataNexusPathKey;
    
    protected String platformId; //平台id

    protected String platformName; //该平台的平台名,需要同蓝鲸的对应的bizName一致

    protected PlatformType type; //平台类型

    protected PlatformFunction func; //平台用途

    protected PlatformCreateMethod createMethod; //平台创建方式

    protected int bkBizId; //该平台对应蓝鲸的bizId

    protected int bkCloudId; //该平台所有服务器所在云

    protected String ccodVersion; //该平台的ccod大版本

    protected List<AppFileNexusInfo> cfgs; //用来存放平台公共配置

    protected String hostUrl; //平台的域名

    protected DatabaseType glsDBType; //ccod平台glsserver的数据库类型

    protected String glsDBUser; //gls数据库的db用户

    protected String glsDBPwd; //gls数据库的登录密码

    protected String baseDataNexusRepository; //基础数据在nexus的存放仓库

    protected String baseDataNexusPath; //基础数据在nexus的存放path

    protected String k8sHostIp; //运行平台的k8s主机ip

    protected String k8sApiUrl; //k8s api的url地址

    protected String k8sAuthToken; //k8s的认证token

    public PlatformBase(){}

    public PlatformBase(PlatformBase platformBase)
    {
        this.platformId = platformBase.platformId;
        this.platformName = platformBase.platformName;
        this.type = platformBase.type;
        this.createMethod = platformBase.createMethod;
        this.func = platformBase.func;
        this.bkBizId = platformBase.bkBizId;
        this.bkCloudId = platformBase.bkCloudId;
        this.ccodVersion = platformBase.ccodVersion;
        this.cfgs = platformBase.cfgs;
        this.hostUrl = platformBase.hostUrl;
        this.glsDBType = platformBase.glsDBType;
        this.glsDBUser = platformBase.glsDBUser;
        this.glsDBPwd = platformBase.glsDBPwd;
        this.baseDataNexusRepository = platformBase.baseDataNexusRepository;
        this.baseDataNexusPath = platformBase.baseDataNexusPath;
        this.k8sHostIp = platformBase.k8sHostIp;
        this.k8sApiUrl = platformBase.k8sApiUrl;
        this.k8sAuthToken = platformBase.k8sAuthToken;
    }

    public PlatformBase(PlatformBase platformBase, Map<String, Object> params)
    {
        this.platformId = platformBase.platformId;
        this.platformName = platformBase.platformName;
        this.type = platformBase.type;
        this.createMethod = platformBase.createMethod;
        this.func = platformBase.func;
        this.bkBizId = platformBase.bkBizId;
        this.bkCloudId = platformBase.bkCloudId;
        this.ccodVersion = platformBase.ccodVersion;
        this.cfgs = platformBase.cfgs;
        this.hostUrl = platformBase.hostUrl;
        this.glsDBType = params.containsKey(this.glsDBTypeKey) ? DatabaseType.getEnum((String)params.get(this.glsDBTypeKey)) : platformBase.glsDBType;
        this.glsDBUser = params.containsKey(this.glsDBUserKey) ? (String)params.get(this.glsDBUserKey) : platformBase.getGlsDBUser();
        this.glsDBPwd = params.containsKey(this.glsDBPwdKey) ? (String)params.get(this.glsDBPwdKey) : platformBase.glsDBPwd;
        this.baseDataNexusRepository = params.containsKey(this.baseDataNexusRepositoryKey) ? (String)params.get(this.baseDataNexusRepositoryKey) : platformBase.baseDataNexusRepository;
        this.baseDataNexusPath = params.containsKey(this.baseDataNexusPathKey) ? (String)params.get(this.baseDataNexusPathKey) : platformBase.baseDataNexusPath;
        this.k8sHostIp = platformBase.k8sHostIp;
        this.k8sApiUrl = platformBase.k8sApiUrl;
        this.k8sAuthToken = platformBase.k8sAuthToken;
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

    public PlatformType getType() {
        return type;
    }

    public void setType(PlatformType type) {
        this.type = type;
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

    public String getK8sHostIp() {
        return k8sHostIp;
    }

    public void setK8sHostIp(String k8sHostIp) {
        this.k8sHostIp = k8sHostIp;
    }

    public String getHostUrl() {
        return hostUrl;
    }

    public void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
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

    public List<AppFileNexusInfo> getCfgs() {
        return cfgs;
    }

    public void setCfgs(List<AppFileNexusInfo> cfgs) {
        this.cfgs = cfgs;
    }

    public PlatformPo getCreatePlatform(String comment)
    {
        PlatformPo po = new PlatformPo(this, CCODPlatformStatus.SCHEMA_CREATE, comment);
        Date now = new Date();
        po.setCreateTime(now);
        po.setUpdateTime(now);
        po.setParams(getPlatformParam());
        return po;
    }

    public Map<String, Object> getPlatformParam()
    {
        Map<String, Object> params = new HashMap<>();
        if(this.glsDBType != null)
            params.put(this.glsDBTypeKey, this.glsDBType.name);
        if(StringUtils.isNotBlank(this.glsDBUser))
            params.put(this.glsDBUserKey, this.glsDBUser);
        if(StringUtils.isNotBlank(this.glsDBPwd))
            params.put(this.glsDBPwdKey, this.glsDBPwd);
        if(this.glsDBType.equals(DatabaseType.ORACLE))
            params.put(this.glsDBSidKey, "xe");
        params.put(this.baseDataNexusPathKey, this.baseDataNexusPath);
        params.put(this.baseDataNexusRepositoryKey, this.baseDataNexusRepository);
        if(type.equals(PlatformType.K8S_CONTAINER))
            params.put(this.k8sHostIpKey, this.k8sHostIp);
        return params;
    }
}
