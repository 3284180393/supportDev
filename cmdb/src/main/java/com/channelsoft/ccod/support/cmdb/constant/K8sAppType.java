package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: K8sAppType
 * @Author: lanhb
 * @Description: 运行在k8s的应用类型枚举类
 * @Date: 2020/6/13 10:30
 * @Version: 1.0
 */
public enum K8sAppType {

    DOMAIN_SERVICE(1, "DOMAIN_SERVICE", "ccod域内部使用服务"),

    DOMAIN_OUT_SERVICE(2, "DOMAIN_SERVICE", "ccod域对外提供的服务"),

    THREE_PART_APP(3, "THREE_PART_APP", "依赖的第三方应用，例如oracle、mysql"),

    THREE_PART_SERVICE(4, "THREE_PART_SERVICE", "依赖的第三方服务，例如umg"),;

    public int id;

    public String name;

    public String desc;

    private K8sAppType(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static K8sAppType getEnumByName(String name)
    {
        if(name == null)
            return null;
        for (K8sAppType type : K8sAppType.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static K8sAppType getEnumById(int id)
    {
        for (K8sAppType type : K8sAppType.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
