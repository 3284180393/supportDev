package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: K8sOperation
 * @Author: lanhb
 * @Description: 用来定义k8s操作类型的枚举
 * @Date: 2020/7/11 17:41
 * @Version: 1.0
 */
public enum K8sOperation {

    CREATE(1, "CREATE", "创建"),

    REPLACE(2, "REPLACE", "替换"),

    DELETE(3, "DELETE", "删除"),;

    public int id;

    public String name;

    public String desc;

    private K8sOperation(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static K8sOperation getEnumByName(String name)
    {
        if(name == null)
            return null;
        for (K8sOperation type : K8sOperation.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static K8sOperation getEnumById(int id)
    {
        for (K8sOperation type : K8sOperation.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
