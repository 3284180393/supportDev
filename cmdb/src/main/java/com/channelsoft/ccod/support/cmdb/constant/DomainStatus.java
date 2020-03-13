package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: DomainStatus
 * @Author: lanhb
 * @Description: 域状态枚举
 * @Date: 2020/3/12 16:52
 * @Version: 1.0
 */
public enum DomainStatus {
    UNKNOWN(0, "UNKNOWN", "未知"),

    RUNNING(1, "RUNNING", "该平台为运营状态,且cmdb和paas数据同步"),

    SUSPEND(2, "SUSPEND", "暂停使用"),

    STOP(3, "STOP", "下架"),

    TRANSFERED(4, "TRANSFERED", "已经被迁移"),;

    public int id;

    public String name;

    public String desc;

    private DomainStatus(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static DomainStatus getEnum(String name)
    {
        if(name == null)
            return null;
        for (DomainStatus type : DomainStatus.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static DomainStatus getEnumById(int id)
    {
        for (DomainStatus type : DomainStatus.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
