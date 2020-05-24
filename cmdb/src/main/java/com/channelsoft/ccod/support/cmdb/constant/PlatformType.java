package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: PlatformType
 * @Author: lanhb
 * @Description: 用来定义平台类型的枚举
 * @Date: 2020/5/24 16:17
 * @Version: 1.0
 */
public enum PlatformType {

    PHYSICAL_MACHINE(1, "PHYSICAL_MACHINE", "由实体机搭建"),

    K8S_CONTAINER(2, "K8S_CONTAINER", "基于k8s的容器化平台"),;

    public int id;

    public String name;

    public String desc;

    private PlatformType(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static PlatformType getEnumByName(String name)
    {
        if(name == null)
            return null;
        for (PlatformType type : PlatformType.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static PlatformType getEnumById(int id)
    {
        for (PlatformType type : PlatformType.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
