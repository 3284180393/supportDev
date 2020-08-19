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
public class PlatformAppPo {

    private int platformAppId; //平台app部署id,数据库唯一生成

    private int appId; //应用id,外键app的appId

    private String appAlias; //应用别名,例如在服务器上部署两个cms,appName=cmsserver,两个cms的别名可以分别取cms1和cms2用来区分

    private String originalAlias; //应用原始别名，客户端提交的未被标准化处理的应用别名

    private String platformId; //平台id

    private String domainId; //应用所在的域id,外键domain的domain_id

    private int assembleId; //应用所在的assemble的id

    private String hostIp; //应用所在服务器的主机ip

    private String port;  //如果应用对外提供服务端口，该值标识端口信息，例如1521:32492/TCP，1521为pod端口，32492为nodePort，

    private int replicas; //运行副本数目

    private int availableReplicas; //可用副本数目

    private String appRunner; //应用运行用户

    private String basePath; //该应用的basePath

    private String deployPath; //部署路径

    private String envLoadCmd; //配置创建命令

    private String initCmd; //初始化命令

    private String startCmd; //启动命令

    private String logOutputCmd; //日志输出命令

    private String ports; //该应用使用的端口

    private String nodePorts; //该应用对外开放的端口

    private String checkAt; //用来定义应用健康检查的端口以及协议

    private String resources; //启动该应用所需的资源

    private int initialDelaySeconds; //应用预计启动时间

    private int periodSeconds; //应用健康检查周期

    private Date deployTime; //该应用的部署路径

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

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public Date getDeployTime() {
        return deployTime;
    }

    public void setDeployTime(Date deployTime) {
        this.deployTime = deployTime;
    }

    public String getAppAlias() {
        return appAlias;
    }

    public void setAppAlias(String appAlias) {
        this.appAlias = appAlias;
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

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
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

    public String getDeployPath() {
        return deployPath;
    }

    public void setDeployPath(String deployPath) {
        this.deployPath = deployPath;
    }

    public String getStartCmd() {
        return startCmd;
    }

    public void setStartCmd(String startCmd) {
        this.startCmd = startCmd;
    }

    public String getInitCmd() {
        return initCmd;
    }

    public void setInitCmd(String initCmd) {
        this.initCmd = initCmd;
    }

    public String getLogOutputCmd() {
        return logOutputCmd;
    }

    public void setLogOutputCmd(String logOutputCmd) {
        this.logOutputCmd = logOutputCmd;
    }

    public String getPorts() {
        return ports;
    }

    public void setPorts(String ports) {
        this.ports = ports;
    }

    public String getNodePorts() {
        return nodePorts;
    }

    public void setNodePorts(String nodePorts) {
        this.nodePorts = nodePorts;
    }

    public String getResources() {
        return resources;
    }

    public void setResources(String resources) {
        this.resources = resources;
    }

    public int getInitialDelaySeconds() {
        return initialDelaySeconds;
    }

    public void setInitialDelaySeconds(int initialDelaySeconds) {
        this.initialDelaySeconds = initialDelaySeconds;
    }

    public int getPeriodSeconds() {
        return periodSeconds;
    }

    public void setPeriodSeconds(int periodSeconds) {
        this.periodSeconds = periodSeconds;
    }

    public String getCheckAt() {
        return checkAt;
    }

    public void setCheckAt(String checkAt) {
        this.checkAt = checkAt;
    }

    public String getEnvLoadCmd() {
        return envLoadCmd;
    }

    public void setEnvLoadCmd(String envLoadCmd) {
        this.envLoadCmd = envLoadCmd;
    }

    public String getPlatformAppDirectory(String appName, String version, PlatformAppPo platformAppPo) {
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String directory = String.format("%s/%s/%s/%s/%s/%s/%s", platformAppPo.getPlatformId(), sf.format(now),
                platformAppPo.getDomainId(), platformAppPo.getHostIp(), appName, platformAppPo.getAppAlias(), version);
        return directory;
    }
}
