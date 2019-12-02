package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: CCODPlatformStatus
 * @Author: lanhb
 * @Description: 用来定义ccod平台状态的枚举
 * @Date: 2019/12/2 16:34
 * @Version: 1.0
 */
public enum CCODPlatformStatus {
    RUNNING(1, "RUNNING", "该平台为运营状态"),

    SUSPEND(2, "SUSPEND", "暂停使用"),

    STOPPING(3, "STOPPING", "下架"),

    NEW_CREATE(3, "NEW_CREATE", "新创建平台"),;

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
}
