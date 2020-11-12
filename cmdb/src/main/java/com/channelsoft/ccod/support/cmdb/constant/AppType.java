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
    BINARY_FILE(1, "BINARY_FILE", "二进制文件"),

    THREE_PART_APP(2, "THREE_PART_APP", "第三方应用,例如oracle、mysql等"),

    RESIN_WEB_APP(3, "RESIN_WEB_APP", "基于resin的web应用"),

    TOMCAT_WEB_APP(4, "TOMCAT_WEB_APP", "基于tomcat的web应用"),

    JAR(5, "JAR", "jar包形式的应用"),

    NODEJS(6, "NODEJS", "nodejs开发的前端应用"),

    OTHER(99, "OTHER", "其它"),;

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
