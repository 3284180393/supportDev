package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: AppRunMethod
 * @Author: lanhb
 * @Description: 用来定义app的运行方式
 * @Date: 2019/11/12 14:39
 * @Version: 1.0
 */
public enum  AppRunMethod {
    SERVICE(1, "SERVICE", "服务"),

    RUNNABLE(2, "RUNNABLE", "可以执行二进制文件"),

    JAR_RUNNABLE(3, "JAR_RUNNABLE", "java可执行文件"),

    JAR_LIB(4, "JAR_LIB", "java lib库"),

    LIB(5, "LIB", "c/c++ lib库"),

    CONTAINER(6, "CONTAINER", "通过容器的方式运行"),

    OTHER(99, "OTHER", "其它");

    public int id;

    public String name;

    public String desc;

    private AppRunMethod(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static AppRunMethod getEnum(String name)
    {
        if(name == null)
            return null;
        for (AppRunMethod type : AppRunMethod.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }
}
