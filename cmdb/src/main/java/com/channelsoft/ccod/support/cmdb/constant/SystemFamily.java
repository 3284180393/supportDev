package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @program: supportDev
 * @ClassName: SystemFamily
 * @author: lanhb
 * @description: 用来定义系统类型的枚举类
 * @create: 2021-02-27 14:47
 **/
public enum SystemFamily {

    CENTOS(1, "CENTOS", "centos system family"),

    SUSE(2, "SUSE", "suse system family"),;

    public int id;

    public String name;

    public String desc;

    private SystemFamily(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static SystemFamily getEnum(String name)
    {
        if(name == null)
            return null;
        for (SystemFamily type : SystemFamily.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

}
