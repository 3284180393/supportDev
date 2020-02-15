package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: DomainType
 * @Author: lanhb
 * @Description: 用来定义域的枚举
 * @Date: 2020/2/15 21:53
 * @Version: 1.0
 */
public enum DomainType {

    SERVICE(1, "SERVICE_DOMAIN", "服务域"),

    PUBLIC_MODULE(2, "PUBLIC_MODULE", "公共组件域"),

    GATE_ACCESS(3, "GATE_ACCESS", "网关接入域"),

    MANAGER_PORTAL(4, "MANAGER_PORTAL", "管理门户域"),

    INTERFACE(5, "INTERFACE", "对外接口域"),

    SUPPORT_PORTAL(6, "SUPPORT_PORTAL", "运营门户域"),

    APG(7, "APG", "自动外拨域"),

    UNKNOWN(0, "UNKNOWN", "未知域"),;

    public int id;

    public String name;

    public String desc;

    private DomainType(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static DomainType getEnum(String name)
    {
        if(name == null)
            return null;
        for (DomainType type : DomainType.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static DomainType getEnumById(int id)
    {
        for (DomainType type : DomainType.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
