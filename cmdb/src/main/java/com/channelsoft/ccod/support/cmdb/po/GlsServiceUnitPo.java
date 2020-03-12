package com.channelsoft.ccod.support.cmdb.po;

import java.io.Serializable;

/**
 * @ClassName: GlsServiceUnitPo
 * @Author: lanhb
 * @Description: 用来定义服务单元的实体类
 * @Date: 2020/3/11 10:43
 * @Version: 1.0
 */
public abstract class GlsServiceUnitPo implements Serializable {

    private int id;

    private String serviceUnitName;

    private int type;

    private int heartBeatTime;

    private int serviceMode;

    private String locationdesc;

    private String domainId;

    private String paramDbName;

    private String paramDbUser;

    private String paramDbPwd;

    private String paramDbIp;

    private String paramVEUrl;

    private String paramUcdsIp;

    private String paramUcdsPort;

    private String paramUcdsMsl;

    private String paramUcdsMtl;

    private String paramUcdsDkl;

    private String paramUcdsHbi;

    private String paramEmsServerLoc;

    private String paramUcdsInnerIp;

    private String paramSsrIp;

    private String paramSsrPort;

    private String description;

    private String insertTime;

    private String operator;

    private String paramDesc;

    private String dcmsId;

    private Short paramDialerResource;

    private String webApps;

    private String getServiceUnitNameEx;

    private String ip;

    private String userName;

    private int blueGreenStatus;

    private static final long serialVersionUID = 1L;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getServiceUnitName() {
        return serviceUnitName;
    }

    public void setServiceUnitName(String serviceUnitName) {
        this.serviceUnitName = serviceUnitName;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getHeartBeatTime() {
        return heartBeatTime;
    }

    public void setHeartBeatTime(int heartBeatTime) {
        this.heartBeatTime = heartBeatTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInsertTime() {
        return insertTime;
    }

    public void setInsertTime(String insertTime) {
        this.insertTime = insertTime;
    }

    public int getServiceMode() {
        return serviceMode;
    }

    public void setServiceMode(int serviceMode) {
        this.serviceMode = serviceMode;
    }

    public String getLocationdesc() {
        return locationdesc;
    }

    public void setLocationdesc(String locationdesc) {
        this.locationdesc = locationdesc;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getDomainId() {
        return domainId;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getParamDbName() {
        return paramDbName;
    }

    public void setParamDbName(String paramDbName) {
        this.paramDbName = paramDbName;
    }

    public String getParamDbUser() {
        return paramDbUser;
    }

    public void setParamDbUser(String paramDbUser) {
        this.paramDbUser = paramDbUser;
    }

    public String getParamDbPwd() {
        return paramDbPwd;
    }

    public void setParamDbPwd(String paramDbPwd) {
        this.paramDbPwd = paramDbPwd;
    }

    public String getParamDbIp() {
        return paramDbIp;
    }

    public void setParamDbIp(String paramDbIp) {
        this.paramDbIp = paramDbIp;
    }

    public String getParamVEUrl() {
        return paramVEUrl;
    }

    public void setParamVEUrl(String paramVEUrl) {
        this.paramVEUrl = paramVEUrl;
    }

    public String getParamDesc() {
        return paramDesc;
    }

    public void setParamDesc(String paramDesc) {
        this.paramDesc = paramDesc;
    }

    public String getParamUcdsIp() {
        return paramUcdsIp;
    }

    public void setParamUcdsIp(String paramUcdsIp) {
        this.paramUcdsIp = paramUcdsIp;
    }

    public String getParamUcdsPort() {
        return paramUcdsPort;
    }

    public void setParamUcdsPort(String paramUcdsPort) {
        this.paramUcdsPort = paramUcdsPort;
    }

    public String getParamUcdsMtl() {
        return paramUcdsMtl;
    }

    public void setParamUcdsMtl(String paramUcdsMtl) {
        this.paramUcdsMtl = paramUcdsMtl;
    }

    public String getParamUcdsMsl() {
        return paramUcdsMsl;
    }

    public void setParamUcdsMsl(String paramUcdsMsl) {
        this.paramUcdsMsl = paramUcdsMsl;
    }

    public String getParamUcdsDkl() {
        return paramUcdsDkl;
    }

    public void setParamUcdsDkl(String paramUcdsDkl) {
        this.paramUcdsDkl = paramUcdsDkl;
    }

    public String getParamUcdsHbi() {
        return paramUcdsHbi;
    }

    public void setParamUcdsHbi(String paramUcdsHbi) {
        this.paramUcdsHbi = paramUcdsHbi;
    }

    public String getDcmsId() {
        return dcmsId;
    }

    public void setDcmsId(String dcmsId) {
        this.dcmsId = dcmsId;
    }

    public String getParamEmsServerLoc() {
        return paramEmsServerLoc;
    }

    public void setParamEmsServerLoc(String paramEmsServerLoc) {
        this.paramEmsServerLoc = paramEmsServerLoc;
    }

    public String getParamUcdsInnerIp() {
        return paramUcdsInnerIp;
    }

    public void setParamUcdsInnerIp(String paramUcdsInnerIp) {
        this.paramUcdsInnerIp = paramUcdsInnerIp;
    }

    public Short getParamDialerResource() {
        return paramDialerResource;
    }

    public void setParamDialerResource(Short paramDialerResource) {
        this.paramDialerResource = paramDialerResource;
    }

    public String getWebApps() {
        return webApps;
    }

    public void setWebApps(String webApps) {
        this.webApps = webApps;
    }

    public String getParamSsrIp() {
        return paramSsrIp;
    }

    public void setParamSsrIp(String paramSsrIp) {
        this.paramSsrIp = paramSsrIp;
    }

    public String getParamSsrPort() {
        return paramSsrPort;
    }

    public void setParamSsrPort(String paramSsrPort) {
        this.paramSsrPort = paramSsrPort;
    }

    public String getGetServiceUnitNameEx() {
        return getServiceUnitNameEx;
    }

    public void setGetServiceUnitNameEx(String getServiceUnitNameEx) {
        this.getServiceUnitNameEx = getServiceUnitNameEx;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getBlueGreenStatus() {
        return blueGreenStatus;
    }

    public void setBlueGreenStatus(int blueGreenStatus) {
        this.blueGreenStatus = blueGreenStatus;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }
}
