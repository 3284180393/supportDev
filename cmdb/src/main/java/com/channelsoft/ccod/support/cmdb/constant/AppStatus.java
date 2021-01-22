package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: AppStatus
 * @Author: lanhb
 * @Description: 用来定义应用状态的枚举
 * @Date: 2021/1/22 10:23
 * @Version: 1.0
 */
public enum AppStatus {

    DEBUG(1, "DEBUG", "测试版"),

    RELEASE(2, "RELEASE", "稳定发布版"),

    ABSENT(3, "ABSENT", "废弃版本"),;

    public int id;

    public String name;

    public String desc;

    private AppStatus(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static AppStatus getEnum(String name)
    {
        if(name == null)
            return null;
        for (AppStatus type : AppStatus.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }
}
