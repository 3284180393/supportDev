package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: CCODPlatformStatus
 * @Author: lanhb
 * @Description: 用来定义ccod平台状态的枚举
 * @Date: 2019/12/2 16:34
 * @Version: 1.0
 */
public enum CCODPlatformStatus {

    UNKNOWN(0, "UNKNOWN", "未知"),

    RUNNING(1, "RUNNING", "该平台为运营状态,且cmdb和paas数据同步"),

    SUSPEND(2, "SUSPEND", "暂停使用"),

    STOP(3, "STOP", "下架"),

    SCHEMA_CREATE_PLATFORM(4, "SCHEMA_CREATE_PLATFORM", "规划中的新建平台"),

    WAIT_SYNC_EXIST_PLATFORM_TO_PAAS(5, "WAIT_SYNC_EXIST_PLATFORM_TO_PAAS", "需要将已有的平台应用部署详情从cmdb同步到paas,cmdb有平台应用部署记录,paas已经创建biz有且仅有idle pool set并且没有分配任何空闲主机"),

    SCHEMA_UPDATE_PLATFORM(6, "SCHEMA_UPDATE_PLATFORM", "有升级计划的平台"),

    PLANING(7, "PLANING", "规划中"),;

    public int id;

    public String name;

    public String desc;

    private CCODPlatformStatus(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static CCODPlatformStatus getEnum(String name)
    {
        if(name == null)
            return null;
        for (CCODPlatformStatus type : CCODPlatformStatus.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static CCODPlatformStatus getEnumById(int id)
    {
        for (CCODPlatformStatus type : CCODPlatformStatus.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
