package com.channelsoft.ccod.support.cmdb.ci.po;

import java.util.Date;
import java.util.Map;

/**
 * @ClassName: BuildDetailPo
 * @Author: lanhb
 * @Description: 用来定义ci build结果的pojo类
 * @Date: 2020/9/27 19:47
 * @Version: 1.0
 */
public class BuildDetailPo {

    private int id;   //id数据库存储主键

    private String jobName;

    private String appName;

    private String version;

    private String projectLeader;

    private Map<String, String> parameters;

    private Date startTime;

    private Date endTime;

    private String jenkinsResult;

    private int queueId; //构建的队列id

    private int number; //构建在jenkins页面#后面的标号

    private String jenkinsLog;

    private String sonarResult;

    private String sonarOutput;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getJenkinsResult() {
        return jenkinsResult;
    }

    public void setJenkinsResult(String jenkinsResult) {
        this.jenkinsResult = jenkinsResult;
    }

    public String getJenkinsLog() {
        return jenkinsLog;
    }

    public void setJenkinsLog(String jenkinsLog) {
        this.jenkinsLog = jenkinsLog;
    }

    public String getSonarResult() {
        return sonarResult;
    }

    public void setSonarResult(String sonarResult) {
        this.sonarResult = sonarResult;
    }

    public String getSonarOutput() {
        return sonarOutput;
    }

    public void setSonarOutput(String sonarOutput) {
        this.sonarOutput = sonarOutput;
    }

    public String getProjectLeader() {
        return projectLeader;
    }

    public void setProjectLeader(String projectLeader) {
        this.projectLeader = projectLeader;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public int getQueueId() {
        return queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }
}
