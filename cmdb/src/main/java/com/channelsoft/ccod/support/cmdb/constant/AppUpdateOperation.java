package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: AppUpdateOperation
 * @Author: lanhb
 * @Description: 用来定义app应用升级操作的枚举
 * @Date: 2019/12/11 10:23
 * @Version: 1.0
 */
public enum AppUpdateOperation {
    ADD(1, "ADD", "添加新应用"),

    DELETE(2, "DELETE", "移除已有应用"),

    VERSION_UPDATE(3, "VERSION_UPDATE", "应用版本更新"),

    CFG_UPDATE(4, "CFG_UPDATE", "配置文件更新"),

    STOP(5, "STOP", "暂停使用"),

    START(6, "STOP", "暂停使用"),;

    public int id;

    public String name;

    public String desc;

    private AppUpdateOperation(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static AppUpdateOperation getEnum(String name)
    {
        if(name == null)
            return null;
        for (AppUpdateOperation type : AppUpdateOperation.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static AppUpdateOperation getEnumById(int id)
    {
        for (AppUpdateOperation type : AppUpdateOperation.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
