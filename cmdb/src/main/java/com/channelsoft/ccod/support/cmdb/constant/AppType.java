package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: AppType
 * @Author: lanhb
 * @Description: 用来定义app类型
 * @Date: 2019/11/12 14:15
 * @Version: 1.0
 */
public enum AppType
{
    CCOD_KERNEL_MODULE(1, "CCOD_KERNEL_MODULE", "ccod核心模块"),

    TOMCAT_APP(2, "TOMCAT_APP", "tomcat应用"),

    RESIN_APP(3, "RESIN_APP", "resin应用"),

    UNDERLYING_MODULE(4, "UNDERLYING_MODULE", "底层模块"),

    THREE_PART_APP(5, "THREE_PART_APP", "第三方应用"),

    ORACLE(6, "ORACLE", "oracle数据库"),

    MYSQL(7, "MYSQL", "mySql数据库"),

    OTHER(99, "OTHER", "其它");

    public int id;

    public String name;

    public String desc;

    private AppType(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static AppType getEnum(String name)
    {
        if(name == null)
            return null;
        for (AppType type : AppType.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }
}
