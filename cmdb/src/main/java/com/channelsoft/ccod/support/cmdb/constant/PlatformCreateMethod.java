package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: PlatformCreateMethod
 * @Author: lanhb
 * @Description: 用来定义平台创建方式的枚举
 * @Date: 2020/6/15 16:16
 * @Version: 1.0
 */
public enum PlatformCreateMethod {

    MANUAL(1, "MANUAL", "手动创建新的平台升级计划"),

    CLONE(2, "CLONE", "从已有的平台克隆而来"),

    PREDEFINE(3, "PREDEFINE", "从预定义脚本创建"),

    ONLINE_MANAGER_COLLECT(4, "ONLINE_MANAGER_COLLECT", "从onlinemanager收集的数据中创建"),

    K8S_API(5, "K8S_API", "从k8s api接口中获取"),

    K8S_API_CLONE(6, "K8S_API_CLONE", "通过k8s api克隆"),;

    public int id;

    public String name;

    public String desc;

    private PlatformCreateMethod(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static PlatformCreateMethod getEnum(String name)
    {
        if(name == null)
            return null;
        for (PlatformCreateMethod type : PlatformCreateMethod.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static PlatformCreateMethod getEnumById(int id)
    {
        for (PlatformCreateMethod type : PlatformCreateMethod.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
