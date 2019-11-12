package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: VersionControl
 * @Author: lanhb
 * @Description: 用来定义模块版本控制方式
 * @Date: 2019/11/12 14:49
 * @Version: 1.0
 */
public enum VersionControl {
    GIT(1, "GIT", "通过git进行版本控制"),

    SVN(2, "SVN", "通过svn进行版本控制"),

    NONE(99, "NONE", "没有版本控制");

    public int id;

    public String name;

    public String desc;

    private VersionControl(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static VersionControl getEnum(String name)
    {
        if(name == null)
            return null;
        for (VersionControl type : VersionControl.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }
}
