package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: AppOperationMethod
 * @Author: lanhb
 * @Description: 用来定义app相关操作的枚举
 * @Date: 2019/11/28 17:11
 * @Version: 1.0
 */
public enum AppOperationMethod {

    ADD_BY_SCAN_NEXUS_REPOSITORY(1, "ADD_BY_SCAN_NEXUS_REPOSITORY", "通过扫描nexus仓库添加");

    public int id;

    public String name;

    public String desc;

    private AppOperationMethod(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static AppOperationMethod getEnum(String name)
    {
        if(name == null)
            return null;
        for (AppOperationMethod type : AppOperationMethod.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static AppOperationMethod getById(int id)
    {
        for (AppOperationMethod type : AppOperationMethod.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
