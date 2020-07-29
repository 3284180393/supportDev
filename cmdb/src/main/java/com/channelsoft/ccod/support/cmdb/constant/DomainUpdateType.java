package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: DomainUpdateType
 * @Author: lanhb
 * @Description: 用来定义域升级类型的枚举
 * @Date: 2019/12/11 15:50
 * @Version: 1.0
 */
public enum DomainUpdateType {

    ADD(1, "ADD", "添加新域"),

    DELETE(2, "DELETE", "移除已有域"),

    UPDATE(3, "UPDATE", "更新已有域"),

    ROLLBACK(4, "ROLLBACK", "将该域回滚到最后一次更新前"),;

    public int id;

    public String name;

    public String desc;

    private DomainUpdateType(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static DomainUpdateType getEnumByName(String name)
    {
        if(name == null)
            return null;
        for (DomainUpdateType type : DomainUpdateType.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static DomainUpdateType getEnumById(int id)
    {
        for (DomainUpdateType type : DomainUpdateType.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
