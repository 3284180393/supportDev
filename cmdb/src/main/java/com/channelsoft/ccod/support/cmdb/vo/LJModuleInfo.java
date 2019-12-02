package com.channelsoft.ccod.support.cmdb.vo;

/**
 * @ClassName: LJModuleInfo
 * @Author: lanhb
 * @Description: 用来定义蓝鲸paas平台的服务器部署模块信息
 * @Date: 2019/12/2 15:41
 * @Version: 1.0
 */
public class LJModuleInfo {
    private int bizId;

    private int setId;

    private int hostId;

    private int moduleId;

    private String moduleName;

    private String version;

    private String ccodVersion;

    private String moduleType;

    public int getBizId() {
        return bizId;
    }

    public void setBizId(int bizId) {
        this.bizId = bizId;
    }

    public int getSetId() {
        return setId;
    }

    public void setSetId(int setId) {
        this.setId = setId;
    }

    public int getHostId() {
        return hostId;
    }

    public void setHostId(int hostId) {
        this.hostId = hostId;
    }

    public int getModuleId() {
        return moduleId;
    }

    public void setModuleId(int moduleId) {
        this.moduleId = moduleId;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getModuleType() {
        return moduleType;
    }

    public void setModuleType(String moduleType) {
        this.moduleType = moduleType;
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
}
