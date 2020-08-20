package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.vo.AppModuleVo;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @ClassName: PlatformAppPo
 * @Author: lanhb
 * @Description: 用来定义平台应用部署情况的类
 * @Date: 2019/11/12 17:04
 * @Version: 1.0
 */
public class PlatformAppPo extends AppBase{

    private int platformAppId; //平台app部署id,数据库唯一生成

    private int appId; //应用id,外键app的appId

    private String alias; //应用别名,例如在服务器上部署两个cms,appName=cmsserver,两个cms的别名可以分别取cms1和cms2用来区分

    private String originalAlias; //应用原始别名，客户端提交的未被标准化处理的应用别名

    private String platformId; //平台id

    private String domainId; //应用所在的域id,外键domain的domain_id

    private int assembleId; //应用所在的assemble的id

    private String hostIp; //应用所在服务器的主机ip

    private int replicas; //运行副本数目

    private int availableReplicas; //可用副本数目

    private String appRunner; //应用运行用户

    private Date deployTime; //该应用的部署路径

    public PlatformAppPo(){}

    public PlatformAppPo(AppBase appBase)
    {
        super(appBase);
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

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }

    public int getAvailableReplicas() {
        return availableReplicas;
    }

    public void setAvailableReplicas(int availableReplicas) {
        this.availableReplicas = availableReplicas;
    }

    public String getPlatformAppDirectory(String appName, String version, PlatformAppPo platformAppPo) {
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String directory = String.format("%s/%s/%s/%s/%s/%s/%s", platformAppPo.getPlatformId(), sf.format(now),
                platformAppPo.getDomainId(), platformAppPo.getHostIp(), appName, platformAppPo.getAlias(), version);
        return directory;
    }
}
