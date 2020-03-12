package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: DatabaseType
 * @Author: lanhb
 * @Description: 用来定义数据库类型的枚举
 * @Date: 2020/3/11 11:46
 * @Version: 1.0
 */
public enum DatabaseType {

    MYSQL(1, "MYSQL", "mysql数据库"),

    ORACLE(2, "ORACLE", "oracle数据库"),

    MONGODB(3, "MONGODB", "mongodb"),;

    public int id;

    public String name;

    public String desc;

    private DatabaseType(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static DatabaseType getEnum(String name)
    {
        if(name == null)
            return null;
        for (DatabaseType type : DatabaseType.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static DatabaseType getEnumById(int id)
    {
        for (DatabaseType type : DatabaseType.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
