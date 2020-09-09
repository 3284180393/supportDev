package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: K8sStatus
 * @Author: lanhb
 * @Description: 用来定义k8s对象状态的枚举
 * @Date: 2020/7/31 19:11
 * @Version: 1.0
 */
public enum K8sStatus {

    ACTIVE(1, "ACTIVE", "活跃状态"),

    UPDATING(2, "UPDATING", "正在更新"),

    ERROR(3, "ERROR", "启动失败"),;

    public int id;

    public String name;

    public String desc;

    private K8sStatus(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static K8sStatus getEnumByName(String name)
    {
        if(name == null)
            return null;
        for (K8sStatus type : K8sStatus.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static K8sStatus getEnumById(int id)
    {
        for (K8sStatus type : K8sStatus.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
