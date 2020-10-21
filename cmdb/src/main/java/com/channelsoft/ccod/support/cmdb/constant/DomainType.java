package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: DomainType
 * @Author: lanhb
 * @Description: 用来定义域的枚举
 * @Date: 2020/2/15 21:53
 * @Version: 1.0
 */
public enum DomainType {

    PHYSICAL_MACHINE(1, "PHYSICAL_MACHINE", "域由实体机搭建"),

    K8S_CONTAINER(2, "K8S_CONTAINER", "域部署在基于k8s容器里"),;

    public int id;

    public String name;

    public String desc;

    private DomainType(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static DomainType getEnum(String name)
    {
        if(name == null)
            return null;
        for (DomainType type : DomainType.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static DomainType getEnumById(int id)
    {
        for (DomainType type : DomainType.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}