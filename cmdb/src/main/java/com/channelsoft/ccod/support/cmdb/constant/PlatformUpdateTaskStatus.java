package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: PlatformUpdateTaskStatus
 * @Author: lanhb
 * @Description: 用来定义平台升级任务状态的枚举
 * @Date: 2019/12/11 9:21
 * @Version: 1.0
 */
public enum PlatformUpdateTaskStatus {

    CREATE(1, "CREATE", "创建"),

    MODIFY(2, "MODIFY", "修改"),

    WAIT_EXEC(3, "WAIT_EXEC", "等待执行"),

    END(4, "END", "升级完成"),;

    public int id;

    public String name;

    public String desc;

    private PlatformUpdateTaskStatus(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static PlatformUpdateTaskStatus getEnumByName(String name)
    {
        if(name == null)
            return null;
        for (PlatformUpdateTaskStatus type : PlatformUpdateTaskStatus.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static PlatformUpdateTaskStatus getEnumById(int id)
    {
        for (PlatformUpdateTaskStatus type : PlatformUpdateTaskStatus.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
