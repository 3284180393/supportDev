package com.channelsoft.ccod.support.cmdb.po;

import java.util.Date;

/**
 * @ClassName: AppTemplatePo
 * @Author: lanhb
 * @Description: 用来定义app模板
 * @Date: 2019/11/12 14:57
 * @Version: 1.0
 */
public class AppTemplatePo {
    private int templateId; // 应用模板定义ID,数据库唯一生成

    private String appType; // app类型参见AppType定义

    private String runMethod; //app的运行方式

    private String versionControl; //app的版本控制方式

    private String appBase; // app缺省所在的顶级目录,例如tomcat的// /home/xxx/../tomcat,ccod运行组件的/home/ccodrunner/Platform,在解析备份时将从此目录开始解析备份

    private String appName; // app的名称例如cmsserver

    private String appAlias; // app的运行别名例如cms

    private String threadFilterRegEx; // app进程过滤正则表达式

    private String relativePort; // app对应端口,如果app正常运行,这些端口将默认打开,多个端口用|连接

    private String profile; // app对应的环境变量文件相对appBase的相对路径

    private String cfgFile; // app对应的cfg文件的绝对/相对路径

    private String logFileDir; // app的日志所在目录的绝对/相对路径

    private String logFileName; // app的当前日志文件的绝对路径,多个用;分割

    private String logFileNameRegEx; // app对应的日志文件名正则表达式

    private String includeDir; // 不在appBase下被包含的目录,多个用;分割

    private String ignoreDir; // 备份时忽略目录,相对目录

    private String ignoreExt; // 忽略文件扩展名

    private String runShell; // 同应用相关脚本

    private String dependences; // 依赖的其它应用

    private Date createTime; // 模板创建时间

    private Date updateTime; // 最后一次修改时间

    private String comment; // 备注

    public int getTemplateId() {
        return templateId;
    }

    public void setTemplateId(int templateId) {
        this.templateId = templateId;
    }

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    public String getRunMethod() {
        return runMethod;
    }

    public void setRunMethod(String runMethod) {
        this.runMethod = runMethod;
    }

    public String getVersionControl() {
        return versionControl;
    }

    public void setVersionControl(String versionControl) {
        this.versionControl = versionControl;
    }

    public String getAppBase() {
        return appBase;
    }

    public void setAppBase(String appBase) {
        this.appBase = appBase;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppAlias() {
        return appAlias;
    }

    public void setAppAlias(String appAlias) {
        this.appAlias = appAlias;
    }

    public String getThreadFilterRegEx() {
        return threadFilterRegEx;
    }

    public void setThreadFilterRegEx(String threadFilterRegEx) {
        this.threadFilterRegEx = threadFilterRegEx;
    }

    public String getRelativePort() {
        return relativePort;
    }

    public void setRelativePort(String relativePort) {
        this.relativePort = relativePort;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getCfgFile() {
        return cfgFile;
    }

    public void setCfgFile(String cfgFile) {
        this.cfgFile = cfgFile;
    }

    public String getLogFileDir() {
        return logFileDir;
    }

    public void setLogFileDir(String logFileDir) {
        this.logFileDir = logFileDir;
    }

    public String getLogFileName() {
        return logFileName;
    }

    public void setLogFileName(String logFileName) {
        this.logFileName = logFileName;
    }

    public String getLogFileNameRegEx() {
        return logFileNameRegEx;
    }

    public void setLogFileNameRegEx(String logFileNameRegEx) {
        this.logFileNameRegEx = logFileNameRegEx;
    }

    public String getIncludeDir() {
        return includeDir;
    }

    public void setIncludeDir(String includeDir) {
        this.includeDir = includeDir;
    }

    public String getIgnoreDir() {
        return ignoreDir;
    }

    public void setIgnoreDir(String ignoreDir) {
        this.ignoreDir = ignoreDir;
    }

    public String getIgnoreExt() {
        return ignoreExt;
    }

    public void setIgnoreExt(String ignoreExt) {
        this.ignoreExt = ignoreExt;
    }

    public String getRunShell() {
        return runShell;
    }

    public void setRunShell(String runShell) {
        this.runShell = runShell;
    }

    public String getDependences() {
        return dependences;
    }

    public void setDependences(String dependences) {
        this.dependences = dependences;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
