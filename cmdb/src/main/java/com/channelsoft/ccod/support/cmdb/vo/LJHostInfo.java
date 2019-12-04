package com.channelsoft.ccod.support.cmdb.vo;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * @ClassName: LJHostInfo
 * @Author: lanhb
 * @Description: 用来定义蓝鲸paas host相关信息
 * @Date: 2019/12/4 13:54
 * @Version: 1.0
 */
public class LJHostInfo {
    private int hostId; //id在蓝鲸存储的主键

    private String mac; //主机mac地址

    private String hostInnerIp; //主机的内部ip

    private String hostOutIp; //主机的外部ip

    private int cpu; //cpu数

    private String cpuMHZ; //cp主频

    private String cpuModule; //cpu module

    private int disk; //磁盘

    private int mem; //内存

    private String osType; //操作系统类型

    private String osName; //操作系统名

    private String osVersion; //操作系统版本

//    private LJBKInfo bizInfo; //host所在biz的信息
//
//    private List<LJSetInfo> sets; //host归属的set
//
//    private List<LJModuleInfo> modules; //服务器安装的应用模块

    public int getHostId() {
        return hostId;
    }

    @JSONField(name = "bk_host_id")
    public void setHostId(int hostId) {
        this.hostId = hostId;
    }

    public String getMac() {
        return mac;
    }

    @JSONField(name = "bk_mac")
    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getHostInnerIp() {
        return hostInnerIp;
    }

    @JSONField(name = "bk_host_innerip")
    public void setHostInnerIp(String hostInnerIp) {
        this.hostInnerIp = hostInnerIp;
    }

    public String getHostOutIp() {
        return hostOutIp;
    }

    @JSONField(name = "bk_host_outerip")
    public void setHostOutIp(String hostOutIp) {
        this.hostOutIp = hostOutIp;
    }

    public int getCpu() {
        return cpu;
    }

    @JSONField(name = "bk_cpu")
    public void setCpu(int cpu) {
        this.cpu = cpu;
    }

    public String getCpuMHZ() {
        return cpuMHZ;
    }

    @JSONField(name = "bk_cpu_mhz")
    public void setCpuMHZ(String cpuMHZ) {
        this.cpuMHZ = cpuMHZ;
    }

    public String getCpuModule() {
        return cpuModule;
    }

    @JSONField(name = "bk_cpu_module")
    public void setCpuModule(String cpuModule) {
        this.cpuModule = cpuModule;
    }

    public int getDisk() {
        return disk;
    }

    @JSONField(name = "bk_disk")
    public void setDisk(int disk) {
        this.disk = disk;
    }

    public int getMem() {
        return mem;
    }

    @JSONField(name = "bk_mem")
    public void setMem(int mem) {
        this.mem = mem;
    }

    public String getOsType() {
        return osType;
    }

    @JSONField(name = "bk_os_type")
    public void setOsType(String osType) {
        this.osType = osType;
    }

    public String getOsName() {
        return osName;
    }

    @JSONField(name = "bk_os_name")
    public void setOsName(String osName) {
        this.osName = osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    @JSONField(name = "bk_os_version")
    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }
}
