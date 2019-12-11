package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: AppUpdateOperationStatus
 * @Author: lanhb
 * @Description: 用来定义应用升级状态的枚举
 * @Date: 2019/12/11 15:46
 * @Version: 1.0
 */
public enum AppUpdateOperationStatus {

    CREATE(1, "CREATE", "创建"),

    MODIFY(2, "MODIFY", "修改"),

    WAIT_EXEC(3, "WAIT_EXEC", "等待执行"),

    END(4, "END", "升级完成"),;

    public int id;

    public String name;

    public String desc;

    private AppUpdateOperationStatus(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static AppUpdateOperationStatus getEnumByName(String name)
    {
        if(name == null)
            return null;
        for (AppUpdateOperationStatus type : AppUpdateOperationStatus.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static AppUpdateOperationStatus getEnumById(int id)
    {
        for (AppUpdateOperationStatus type : AppUpdateOperationStatus.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
