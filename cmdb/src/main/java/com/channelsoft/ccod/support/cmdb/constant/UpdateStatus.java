package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: UpdateStatus
 * @Author: lanhb
 * @Description: 用来定义升级状态的枚举
 * @Date: 2019/12/11 16:38
 * @Version: 1.0
 */
public enum UpdateStatus {
    CREATE(1, "CREATE", "创建"),

    MODIFY(2, "MODIFY", "修改"),

    WAIT_EXEC(3, "WAIT_EXEC", "等待执行"),

    SUCCESS(4, "SUCCESS", "升级成功"),

    FAIL(5, "FAIL", "升级失败"),

    CANCEL(6, "CANCEL", "升级取消"),

    EXEC(7, "EXEC", "执行部署"),;

    public int id;

    public String name;

    public String desc;

    private UpdateStatus(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static UpdateStatus getEnumByName(String name)
    {
        if(name == null)
            return null;
        for (UpdateStatus type : UpdateStatus.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static UpdateStatus getEnumById(int id)
    {
        for (UpdateStatus type : UpdateStatus.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
