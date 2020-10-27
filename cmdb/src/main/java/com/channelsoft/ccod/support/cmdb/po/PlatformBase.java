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

    public static String glsDBTypeKey = "glsDBType";

    public static String glsDBUserKey = "glsDBUser";

    public static String glsDBPwdKey = "glsDBPwd";

    public static String glsDBSidKey = "glsDBSid";

    public static String k8sHostIpKey = "k8sHostIp";

    public static String nfsServerIpKey = "nfsServerIp";

    public static String dbPortKey = "dbPort";

    public static String baseDataNexusRepositoryKey = "baseDataNexusRepository";

    public static String baseDataNexusPathKey = "baseDataNexusPath";

    public static String statusBeforeDebugKey = "statusBeforeDebugKey";

    protected String platformId; //平台id

    protected String platformName; //该平台的平台名

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

    protected String nfsServerIp; //平台被挂载的nfs的server ip

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
        this.nfsServerIp = platformBase.nfsServerIp;
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
        this.glsDBType = platformBase.glsDBType != null ? platformBase.glsDBType : DatabaseType.getEnum((String)params.get(glsDBTypeKey));
        this.glsDBUser = StringUtils.isNotBlank(platformBase.glsDBUser) ? platformBase.glsDBUser : (String)params.get(glsDBUserKey);
        this.glsDBPwd = StringUtils.isNotBlank(platformBase.glsDBPwd) ? platformBase.glsDBPwd : (String)params.get(glsDBPwdKey);
        this.baseDataNexusRepository = StringUtils.isNotBlank(platformBase.baseDataNexusRepository) ? platformBase.baseDataNexusRepository : (String)params.get(baseDataNexusRepositoryKey);
        this.baseDataNexusPath = StringUtils.isNotBlank(platformBase.baseDataNexusPath) ? platformBase.baseDataNexusPath : (String)params.get(this.baseDataNexusPathKey);
        this.k8sHostIp = StringUtils.isNotBlank(platformBase.k8sHostIp) ? platformBase.k8sHostIp : (String)params.get(k8sHostIpKey);
        this.k8sApiUrl = platformBase.k8sApiUrl;
        this.k8sAuthToken = platformBase.k8sAuthToken;
        this.nfsServerIp = StringUtils.isNotBlank(platformBase.nfsServerIp) ? platformBase.nfsServerIp : (String)params.get(nfsServerIpKey);
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

    public String getNfsServerIp() {
        return nfsServerIp;
    }

    public void setNfsServerIp(String nfsServerIp) {
        this.nfsServerIp = nfsServerIp;
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
        else if(this.glsDBType.equals(DatabaseType.MYSQL))
            params.put(this.glsDBSidKey, "ucds");
        params.put(this.baseDataNexusPathKey, this.baseDataNexusPath);
        params.put(this.baseDataNexusRepositoryKey, this.baseDataNexusRepository);
        if(type.equals(PlatformType.K8S_CONTAINER))
            params.put(this.k8sHostIpKey, this.k8sHostIp);
        if(type.equals(PlatformType.K8S_CONTAINER))
            params.put(this.nfsServerIp, StringUtils.isNotBlank(this.nfsServerIp) ? this.nfsServerIp : this.k8sHostIp);
        return params;
    }
}
