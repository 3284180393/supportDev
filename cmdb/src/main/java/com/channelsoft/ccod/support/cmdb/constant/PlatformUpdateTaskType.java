package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: PlatformUpdateTaskType
 * @Author: lanhb
 * @Description: 用来定义平台升级任务类型的枚举类型
 * @Date: 2019/12/11 9:20
 * @Version: 1.0
 */
public enum PlatformUpdateTaskType {

    CREATE(1, "CREATE", "创建新平台"),

    DELETE(2, "DELETE", "下架已有平台"),

    UPDATE(3, "UPDATE", "更新已有平台"),;

    public int id;

    public String name;

    public String desc;

    private PlatformUpdateTaskType(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static PlatformUpdateTaskType getEnumByName(String name)
    {
        if(name == null)
            return null;
        for (PlatformUpdateTaskType type : PlatformUpdateTaskType.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static PlatformUpdateTaskType getEnumById(int id)
    {
        for (PlatformUpdateTaskType type : PlatformUpdateTaskType.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
