package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: PlatformFunction
 * @Author: lanhb
 * @Description: 用来定义平台功能的枚举类
 * @Date: 2020/6/15 14:04
 * @Version: 1.0
 */
public enum PlatformFunction {

    ONLINE(1, "ONLINE", "在线运营平台"),

    TEST(2, "TEST", "测试平台"),

    PROTOTYPE(999, "PROTOTYPE", "原型平台"),;

    public int id;

    public String name;

    public String desc;

    private PlatformFunction(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static PlatformFunction getEnum(String name)
    {
        if(name == null)
            return null;
        for (PlatformFunction type : PlatformFunction.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static PlatformFunction getEnumById(int id)
    {
        for (PlatformFunction type : PlatformFunction.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }

}
