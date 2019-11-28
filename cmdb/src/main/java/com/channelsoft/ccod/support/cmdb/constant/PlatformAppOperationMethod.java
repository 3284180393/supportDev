package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: PlatformAppOperationMethod
 * @Author: lanhb
 * @Description: 用来定义处理平台应用信息的方式的枚举
 * @Date: 2019/11/28 16:22
 * @Version: 1.0
 */
public enum PlatformAppOperationMethod {

    ADD_BY_PLATFORM_CLIENT_COLLECT(1, "ADD_BY_PLATFORM_CLIENT_COLLECT", "通过扫描应用发布nexus仓库添加");

    public int id;

    public String name;

    public String desc;

    private PlatformAppOperationMethod(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static PlatformAppOperationMethod getEnum(String name)
    {
        if(name == null)
            return null;
        for (PlatformAppOperationMethod type : PlatformAppOperationMethod.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static PlatformAppOperationMethod getById(int id)
    {
        for (PlatformAppOperationMethod type : PlatformAppOperationMethod.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
