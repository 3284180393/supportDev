package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: ServiceAccessMethod
 * @Author: lanhb
 * @Description: 用来定义服务访问模式的枚举
 * @Date: 2020/11/6 16:38
 * @Version: 1.0
 */
public enum  ServiceAccessMethod {

    SERVICE(1, "SERVICE", "通过访问k8s命名空间内部服务的方式访问服务"),

    ENTITY(2, "ENTITY", "直接访问实体机上的服务"),

    CLOUD(3, "CLOUD", "通过云的方式访问，例如其它命名空间提供的服务"),;

    public int id;

    public String name;

    public String desc;

    private ServiceAccessMethod(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static ServiceAccessMethod getEnumByName(String name)
    {
        if(name == null)
            return null;
        for (ServiceAccessMethod type : ServiceAccessMethod.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static ServiceAccessMethod getEnumById(int id)
    {
        for (ServiceAccessMethod type : ServiceAccessMethod.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
