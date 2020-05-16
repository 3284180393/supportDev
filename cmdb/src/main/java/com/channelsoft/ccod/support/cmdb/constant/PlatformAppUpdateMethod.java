package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: PlatformAppUpdateMethod
 * @Author: lanhb
 * @Description: 用来定义平台应用更新方式的枚举
 * @Date: 2020/5/15 18:00
 * @Version: 1.0
 */
public enum PlatformAppUpdateMethod {

    RUNNING(1, "RUNNING", "该平台为运营状态,且cmdb和paas数据同步"),

    SUSPEND(2, "SUSPEND", "暂停使用"),

    STOP(3, "STOP", "下架"),

    TRANSFERED(4, "TRANSFERED", "已经被迁移"),;

    public int id;

    public String name;

    public String desc;

    private PlatformAppUpdateMethod(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static PlatformAppUpdateMethod getEnum(String name)
    {
        if(name == null)
            return null;
        for (PlatformAppUpdateMethod type : PlatformAppUpdateMethod.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static PlatformAppUpdateMethod getEnumById(int id)
    {
        for (PlatformAppUpdateMethod type : PlatformAppUpdateMethod.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
