package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: K8sKind
 * @Author: lanhb
 * @Description: 用来定义k8s的kind枚举
 * @Date: 2020/5/27 9:49
 * @Version: 1.0
 */
public enum K8sKind {

    POD(1, "POD", "pod of k8s"),

    SERVICE(2, "SERVICE", "service of k8s"),

    NAMESPACE(3, "NAMESPACE", "namespace of k8s"),

    DEPLOYMENT(4, "DEPLOYMENT", "deployment of k8s"),

    INGRESS(5, "INGRESS", "ingress of k8s"),

    SECRET(6, "SECRET", "secret of k8s"),

    PV(7, "PV", "pv of k8s"),

    PVC(8, "PVC", "pvc of k8s"),

    CONFIGMAP(9, "CONFIGMAP", "configMap of k8s"),

    JOB(10, "JOB", "job of k8s"),

    ENDPOINTS(11, "ENDPOINTS", "end points of k8s"),;

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
