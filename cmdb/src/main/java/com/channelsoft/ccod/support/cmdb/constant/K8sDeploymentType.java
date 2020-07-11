package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: K8sDeploymentType
 * @Author: lanhb
 * @Description: 用来定义k8s deployment上面部署的应用类型
 * @Date: 2020/7/11 12:17
 * @Version: 1.0
 */
public enum K8sDeploymentType {

    CCOD_DOMAIN_APP(1, "CCOD_DOMAIN_APP", "deployment定义的是ccod域应用"),

    THREE_PART_APP(2, "THREE_PART_APP", "deployment定义的是系统依赖的第三方应用，例如oracle、mysql"),;

    public int id;

    public String name;

    public String desc;

    private K8sDeploymentType(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static K8sDeploymentType getEnumByName(String name)
    {
        if(name == null)
            return null;
        for (K8sDeploymentType type : K8sDeploymentType.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static K8sDeploymentType getEnumById(int id)
    {
        for (K8sDeploymentType type : K8sDeploymentType.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
