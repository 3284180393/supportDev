package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: K8sKind
 * @Author: lanhb
 * @Description: 用来定义k8s的kind枚举
 * @Date: 2020/5/27 9:49
 * @Version: 1.0
 */
public enum K8sKind {

    Pod(1, "Pod", "pod of k8s"),

    Service(2, "Service", "service of k8s"),

    Namespace(3, "Namespace", "namespace of k8s"),

    ReplicaSet(4, "ReplicaSet", "replicaSet of k8s"),;

    public int id;

    public String name;

    public String desc;

    private K8sKind(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static K8sKind getEnumByName(String name)
    {
        if(name == null)
            return null;
        for (K8sKind type : K8sKind.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static K8sKind getEnumById(int id)
    {
        for (K8sKind type : K8sKind.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
