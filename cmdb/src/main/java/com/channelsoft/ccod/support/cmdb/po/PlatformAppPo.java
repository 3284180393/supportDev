package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.vo.AppFileNexusInfo;
import com.channelsoft.ccod.support.cmdb.vo.AppModuleVo;
import com.channelsoft.ccod.support.cmdb.vo.AppUpdateOperationInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @ClassName: PlatformAppPo
 * @Author: lanhb
 * @Description: 用来定义平台应用部署情况的类
 * @Date: 2019/11/12 17:04
 * @Version: 1.0
 */
public class PlatformAppPo extends AppBase{

    protected int platformAppId; //平台app部署id,数据库唯一生成

    protected int appId; //应用id,外键app的appId

    protected String originalAlias; //应用原始别名，客户端提交的未被标准化处理的应用别名

    protected String platformId; //平台id

    protected String domainId; //应用所在的域id,外键domain的domain_id

    protected int assembleId; //应用所在的assemble的id

    protected String hostIp; //应用所在服务器的主机ip

    protected boolean fixedIp; //ip是否固定

    protected String appRunner; //应用运行用户

    protected Date deployTime; //该应用的部署路径

    protected String tag; //应用标签

    public PlatformAppPo(){}

    public PlatformAppPo(AppBase appBase, int appId, List<AppFileNexusInfo> cfgs, String platformId, String domainId, int assembleId, String originalAlias,
                         String hostIp, boolean fixedIp, String appRunner, String tag)
    {
        super(appBase);
        this.cfgs = cfgs;
        this.appId = appId;
        this.platformId = platformId;
        this.domainId = domainId;
        this.originalAlias = originalAlias;
        this.assembleId = assembleId;
        this.hostIp = hostIp;
        this.fixedIp = fixedIp;
        this.appRunner = appRunner;
        this.deployTime = new Date();
        this.tag = tag;
    }

    public void update(AppBase appBase, int appId, List<AppFileNexusInfo> cfgs, String hostIp, String tag)
    {
        changeTo(appBase);
        this.cfgs = cfgs;
        this.appId = appId;
        this.hostIp = hostIp;
        this.tag = tag;
    }

    public int getPlatformAppId() {
        return platformAppId;
    }

    public void setPlatformAppId(int platformAppId) {
        this.platformAppId = platformAppId;
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

    public int getAssembleId() {
        return assembleId;
    }

    public void setAssembleId(int assembleId) {
        this.assembleId = assembleId;
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public Date getDeployTime() {
        return deployTime;
    }

    public void setDeployTime(Date deployTime) {
        this.deployTime = deployTime;
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

    public String getOriginalAlias() {
        return originalAlias;
    }

    public void setOriginalAlias(String originalAlias) {
        this.originalAlias = originalAlias;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getPlatformAppDirectory(String appName, String version, PlatformAppPo platformAppPo) {
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String directory = String.format("%s/%s/%s/%s/%s/%s/%s", platformAppPo.getPlatformId(), sf.format(now),
                platformAppPo.getDomainId(), platformAppPo.getHostIp(), appName, platformAppPo.getAlias(), version);
        return directory;
    }
}
