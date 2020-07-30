package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: ServicePortType
 * @Author: lanhb
 * @Description: 用来定义服务类型端口类型的枚举
 * @Date: 2020/7/30 10:18
 * @Version: 1.0
 */
public enum ServicePortType {

    ClusterIP(1, "ClusterIP", "ClusterIP"),

    NodePort(2, "NodePort", "NodePort"),

    LoadBalancer(3, "LoadBalancer", "LoadBalancer"),

    ExternalName(4, "ExternalName", "ExternalName"),;

    public int id;

    public String name;

    public String desc;

    private ServicePortType(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static ServicePortType getEnumByName(String name)
    {
        if(name == null)
            return null;
        for (ServicePortType type : ServicePortType.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static ServicePortType getEnumById(int id)
    {
        for (ServicePortType type : ServicePortType.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
