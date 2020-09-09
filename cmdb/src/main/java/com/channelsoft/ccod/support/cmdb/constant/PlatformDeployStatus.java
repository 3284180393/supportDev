package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: PlatformDeployStatus
 * @Author: lanhb
 * @Description: 用来定义平台部署状态的枚举
 * @Date: 2020/9/8 19:03
 * @Version: 1.0
 */
public enum PlatformDeployStatus {

    DEPLOYING(1, "DEPLOYING", "正在部署"),

    SUCCESS(2, "SUCCESS", "部署成功"),

    FAIL(3, "FAIL", "部署失败"),

    NOT_EXEC(4, "NOT_EXEC", "未执行");

    public int id;

    public String name;

    public String desc;

    private PlatformDeployStatus(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static PlatformDeployStatus getEnumByName(String name)
    {
        if(name == null)
            return null;
        for (PlatformDeployStatus type : PlatformDeployStatus.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static PlatformDeployStatus getEnumById(int id)
    {
        for (PlatformDeployStatus type : PlatformDeployStatus.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
