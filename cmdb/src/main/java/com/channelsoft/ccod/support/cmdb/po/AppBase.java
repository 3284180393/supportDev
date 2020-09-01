package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.constant.DatabaseType;
import com.channelsoft.ccod.support.cmdb.vo.AppFileNexusInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: AppBase
 * @Author: lanhb
 * @Description: 用来定义CCOD应用基础属性的基类
 * @Date: 2020/8/20 15:49
 * @Version: 1.0
 */
public abstract class AppBase {

    protected String appName; //应用名

    protected String alias; //应用的别名

    protected AppType appType; //应用类型

    protected String version; //应用版本

    protected String ccodVersion; //归属于哪个大版本的ccod

    protected String basePath; //缺省的base path

    protected String deployPath; //应用程序/包相对basePath的路径

    protected String envLoadCmd; //配置创建命令

    protected String initCmd; //初始化命令

    protected String startCmd; //启动命令

    protected String logOutputCmd; //日志输出命令

    protected String ports; //该应用使用的端口

    protected String nodePorts; //该应用对外开放的端口

    protected String checkAt; //用来定义应用健康检查的端口以及协议

    protected String resources; //启动该应用所需的资源

    protected int initialDelaySeconds; //应用预计启动时间

    protected int periodSeconds; //应用健康检查周期

    protected List<AppFileNexusInfo> cfgs; //应用配置文件

    protected AppFileNexusInfo installPackage; //应用安装包

    public AppBase(){}

    public AppBase(AppBase appBase)
    {
        this.appName = appBase.appName;
        this.alias = appBase.alias;
        this.version = appBase.version;
        this.appType = appBase.appType;
        this.basePath = appBase.basePath;
        this.ccodVersion = appBase.ccodVersion;
        this.checkAt = appBase.checkAt;
        this.deployPath = appBase.deployPath;
        this.envLoadCmd = appBase.envLoadCmd;
        this.initCmd = appBase.initCmd;
        this.initialDelaySeconds = appBase.initialDelaySeconds;
        this.logOutputCmd = appBase.logOutputCmd;
        this.nodePorts = appBase.nodePorts;
        this.periodSeconds = appBase.periodSeconds;
        this.ports = appBase.ports;
        this.resources = appBase.resources;
        this.startCmd = appBase.startCmd;
        this.cfgs = appBase.cfgs;
        this.installPackage = appBase.installPackage;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public AppType getAppType() {
        return appType;
    }

    public void setAppType(AppType appType) {
        this.appType = appType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCcodVersion() {
        return ccodVersion;
    }

    public void setCcodVersion(String ccodVersion) {
        this.ccodVersion = ccodVersion;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getDeployPath() {
        return deployPath;
    }

    public void setDeployPath(String deployPath) {
        this.deployPath = deployPath;
    }

    public String getEnvLoadCmd() {
        return envLoadCmd;
    }

    public void setEnvLoadCmd(String envLoadCmd) {
        this.envLoadCmd = envLoadCmd;
    }

    public String getInitCmd() {
        return initCmd;
    }

    public void setInitCmd(String initCmd) {
        this.initCmd = initCmd;
    }

    public String getStartCmd() {
        return startCmd;
    }

    public void setStartCmd(String startCmd) {
        this.startCmd = startCmd;
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

    public String getCheckAt() {
        return checkAt;
    }

    public void setCheckAt(String checkAt) {
        this.checkAt = checkAt;
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

    public List<AppFileNexusInfo> getCfgs() {
        return cfgs;
    }

    public void setCfgs(List<AppFileNexusInfo> cfgs) {
        this.cfgs = cfgs;
    }

    public AppFileNexusInfo getInstallPackage() {
        return installPackage;
    }

    public void setInstallPackage(AppFileNexusInfo installPackage) {
        this.installPackage = installPackage;
    }

    public String getAppNexusDirectory() {
        String directory = String.format("%s/%s", this.appName, this.version);
        return directory;
    }

    public String getAppNexusGroup()
    {
        String group = String.format("/%s/%s", this.appName, this.version);
        return group;
    }

    public void changeTo(AppBase appBase)
    {
        this.version = StringUtils.isNotBlank(appBase.version) ? appBase.version : this.version;
        this.basePath = StringUtils.isNotBlank(appBase.basePath) ? appBase.basePath : this.basePath;
        this.checkAt = StringUtils.isNotBlank(appBase.checkAt) ? appBase.checkAt : this.checkAt;
        this.deployPath = StringUtils.isNotBlank(appBase.deployPath) ? appBase.deployPath : this.deployPath;
        this.envLoadCmd = StringUtils.isNotBlank(appBase.envLoadCmd) ? appBase.envLoadCmd : this.envLoadCmd;
        this.initCmd = StringUtils.isNotBlank(appBase.initCmd) ? appBase.initCmd : this.initCmd;
        this.initialDelaySeconds = appBase.initialDelaySeconds > 0 ? appBase.initialDelaySeconds : this.initialDelaySeconds;
        this.logOutputCmd = StringUtils.isNotBlank(appBase.logOutputCmd) ? appBase.logOutputCmd : this.logOutputCmd;
        this.nodePorts = StringUtils.isNotBlank(appBase.nodePorts) ? appBase.nodePorts : this.nodePorts;
        this.periodSeconds = appBase.periodSeconds > 0 ? appBase.periodSeconds : this.periodSeconds;
        this.ports = StringUtils.isNotBlank(appBase.ports) ? appBase.ports : this.ports;
        this.resources = StringUtils.isNotBlank(appBase.resources) ? appBase.resources : this.resources;
        this.startCmd = StringUtils.isNotBlank(appBase.startCmd) ? appBase.startCmd : this.startCmd;
        this.cfgs = appBase.cfgs != null && appBase.cfgs.size() > 0 ? appBase.cfgs : this.cfgs;
        this.installPackage = appBase.installPackage != null ? appBase.installPackage : this.installPackage;
    }

    public void fill(AppBase appBase)
    {
        this.version = StringUtils.isBlank(this.version) ? appBase.version : this.version;
        this.basePath = StringUtils.isBlank(this.basePath) ? appBase.basePath : this.basePath;
        this.checkAt = StringUtils.isBlank(this.checkAt) ? appBase.checkAt : this.checkAt;
        this.deployPath = StringUtils.isBlank(this.deployPath) ? appBase.deployPath : this.deployPath;
        this.envLoadCmd = StringUtils.isBlank(this.envLoadCmd) ? appBase.envLoadCmd : this.envLoadCmd;
        this.initCmd = StringUtils.isBlank(this.initCmd) ? appBase.initCmd : this.initCmd;
        this.initialDelaySeconds = this.initialDelaySeconds <= 0 ? appBase.initialDelaySeconds : this.initialDelaySeconds;
        this.logOutputCmd = StringUtils.isBlank(this.logOutputCmd) ? appBase.logOutputCmd : this.logOutputCmd;
        this.nodePorts = StringUtils.isBlank(this.nodePorts) ? appBase.nodePorts : this.nodePorts;
        this.periodSeconds = this.periodSeconds <= 0 ? appBase.periodSeconds : this.periodSeconds;
        this.ports = StringUtils.isBlank(this.ports) ? appBase.ports : this.ports;
        this.resources = StringUtils.isBlank(this.resources) ? appBase.resources : this.resources;
        this.startCmd = StringUtils.isBlank(this.startCmd) ? appBase.startCmd : this.startCmd;
        this.cfgs = this.cfgs == null || this.cfgs.size() == 0 ? appBase.cfgs : this.cfgs;
        this.installPackage = this.installPackage == null ? appBase.installPackage : this.installPackage;
    }
}
