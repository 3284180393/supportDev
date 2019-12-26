package com.channelsoft.ccod.support.cmdb.vo;

/**
 * @ClassName: CCODHostInfo
 * @Author: lanhb
 * @Description: 用来定义ccod的主机信息
 * @Date: 2019/12/4 9:43
 * @Version: 1.0
 */
public class CCODHostInfo {

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

    public CCODHostInfo(LJHostInfo ljHostInfo)
    {
        this.cpu = ljHostInfo.getCpu();
        this.cpuMHZ = ljHostInfo.getCpuMHZ();
        this.cpuModule = ljHostInfo.getCpuModule();
        this.disk = ljHostInfo.getDisk();
        this.hostInnerIp = ljHostInfo.getHostInnerIp();
        this.hostOutIp = ljHostInfo.getHostOutIp();
        this.mac = ljHostInfo.getMac();
        this.mem = ljHostInfo.getMem();
        this.osName = ljHostInfo.getOsName();
        this.osType = ljHostInfo.getOsType();
        this.osVersion = ljHostInfo.getOsVersion();
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getHostInnerIp() {
        return hostInnerIp;
    }

    public void setHostInnerIp(String hostInnerIp) {
        this.hostInnerIp = hostInnerIp;
    }

    public String getHostOutIp() {
        return hostOutIp;
    }

    public void setHostOutIp(String hostOutIp) {
        this.hostOutIp = hostOutIp;
    }

    public int getCpu() {
        return cpu;
    }

    public void setCpu(int cpu) {
        this.cpu = cpu;
    }

    public String getCpuMHZ() {
        return cpuMHZ;
    }

    public void setCpuMHZ(String cpuMHZ) {
        this.cpuMHZ = cpuMHZ;
    }

    public String getCpuModule() {
        return cpuModule;
    }

    public void setCpuModule(String cpuModule) {
        this.cpuModule = cpuModule;
    }

    public int getDisk() {
        return disk;
    }

    public void setDisk(int disk) {
        this.disk = disk;
    }

    public int getMem() {
        return mem;
    }

    public void setMem(int mem) {
        this.mem = mem;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public String getOsName() {
        return osName;
    }

    public void setOsName(String osName) {
        this.osName = osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

}
